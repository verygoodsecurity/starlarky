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
