// Extensions compiled into the pinned Kitesurf core by build.sh.
#[cfg(target_os = "wasi")]
impl DomHandle {
    pub fn wasi_attribute(&self, handle: u32, name: &str) -> Option<String> {
        let node = self.resolve_node_id(handle)?;
        let doc = self.doc.try_borrow().ok()?;
        let data = doc.get_node(node)?.element_data()?;
        data.attrs.iter().find(|attr| {
            attr.prefix().is_none() && &*attr.name.local == name
        }).map(|attr| attr.value.to_string())
    }

    pub fn wasi_filled(&self, handle: u32) -> bool {
        let Some(node) = self.resolve_node_id(handle) else { return false; };
        let Ok(doc) = self.doc.try_borrow() else { return false; };
        let Some(data) = doc.get_node(node).and_then(|node| node.element_data()) else {
            return false;
        };
        data.text_input_data().and_then(|input| input.value_override.as_ref())
            .is_some_and(|value| !value.is_empty())
    }

}

#[cfg(target_os = "wasi")]
impl DomHandle {
    pub fn wasi_node_info(&self, handle: u32) -> Option<serde_json::Value> {
        let id = self.resolve_node_id(handle)?;
        let doc = self.doc.try_borrow().ok()?;
        let node = doc.get_node(id)?;
        let identity = serde_json::to_value(crate::dom::cdp::describe_node_identity(node)).ok()?;
        let parent = node.parent_id().map(|id| self.external_node_id(id));
        let children: Vec<_> = node.children().into_iter().map(|id| self.external_node_id(id)).collect();
        Some(serde_json::json!({
            "type": identity["nodeType"],
            "name": identity["nodeName"],
            "localName": identity["localName"],
            "value": identity["nodeValue"],
            "namespaceURI": identity["namespaceURI"],
            "parent": parent,
            "children": children,
        }))
    }

    pub fn wasi_input_value(&self, handle: u32) -> Option<String> {
        let id = self.resolve_node_id(handle)?;
        let doc = self.doc.try_borrow().ok()?;
        let node = doc.get_node(id)?;
        let data = node.element_data()?;
        let explicit = data.text_input_data().and_then(|input| input.value_override.clone());
        if explicit.is_some() {
            return explicit;
        }
        if &*data.name.local == "textarea" {
            return Some(node.text_content());
        }
        data.attrs.iter()
            .find(|attr| attr.prefix().is_none() && &*attr.name.local == "value")
            .map(|attr| attr.value.to_string())
    }

    pub fn wasi_set_html(&self, handle: u32, html: &str) -> bool {
        let Some(id) = self.resolve_node_id(handle) else { return false; };
        let Ok(mut doc) = self.doc.try_borrow_mut() else { return false; };
        if doc.get_node(id).is_none() {
            return false;
        }
        doc.mutate().set_inner_html(id, html);
        self.layout_dirty.set(true);
        true
    }
}
