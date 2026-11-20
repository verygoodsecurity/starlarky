use crate::{
    host,
    page::{Message, Messages, PageScript},
};
use kitesurf_core::DomHandle;
use serde_json::{json, Value};
use std::cell::{Cell, RefCell};
use std::collections::{HashMap, VecDeque};
use std::rc::Rc;
use url::Url;

const MAX_DOCUMENTS: usize = 12;
const MAX_FIELDS: usize = 100;
const MAX_ACTIONS: usize = 40;
const MAX_SCRIPTS_PER_DOCUMENT: usize = 80;
const MAX_TURNS: usize = 128;

thread_local! {
    // Handles from a navigated-away page cannot refer to a later document's fields.
    static NEXT_DOCUMENT: Cell<u32> = const { Cell::new(0) };
}

pub struct Browser {
    pages: Vec<Document>,
    messages: Messages,
}

struct Document {
    id: u32,
    url: Url,
    origin: String,
    dom: Rc<DomHandle>,
    script: Option<PageScript>,
    parent: Option<(u32, u32)>,
}

impl Browser {
    pub fn open(url: &str) -> Result<Self, &'static str> {
        if url.is_empty() || url.len() > 4096 {
            return Err("invalid document URL");
        }
        let url = Url::parse(url).map_err(|_| "invalid document URL")?;
        let messages = Rc::new(RefCell::new(VecDeque::new()));
        let root = Document::open(url, None, true, true, &messages)?;
        let mut browser = Self {
            pages: vec![root],
            messages,
        };
        browser.run();
        Ok(browser)
    }

    pub fn inspect(&mut self) -> Result<Value, &'static str> {
        self.settle()?;
        let root = self.pages.first().ok_or("open a document first")?;
        let title = root
            .query("title")?
            .first()
            .map(|node| root.dom.text_content(*node))
            .unwrap_or_default();
        let mut fields = Vec::new();
        let mut actions = Vec::new();
        for page in &self.pages {
            page.fields(&mut fields)?;
            page.actions(&mut actions)?;
        }
        Ok(json!({
            "title": bounded(&title, 200),
            "script_count": root.query("script")?.len(),
            "iframe_count": root.query("iframe")?.len(),
            "loaded_frame_count": self.pages.len() - 1,
            "fields": fields,
            "actions": actions,
        }))
    }

    pub fn fill(&mut self, command: &Value) -> Result<Value, &'static str> {
        if command.get("target") != Some(&self.field_target(command)?) {
            return Err("browser field target changed");
        }
        let (page, node) = self.lookup(command)?;
        let value = command
            .get("value")
            .and_then(Value::as_str)
            .filter(|value| value.len() <= 1024)
            .ok_or("invalid field value")?;
        if let Some(script) = &mut page.script {
            if !page.dom.set_input_text(node, "") {
                return Err("field cannot be filled");
            }
            for character in value.chars() {
                let key = character.to_string();
                if script.keydown(node, &key)? {
                    let mut updated = page.dom.wasi_input_value(node).unwrap_or_default();
                    updated.push(character);
                    if !page.dom.set_input_text(node, &updated) {
                        return Err("field cannot be filled");
                    }
                    script.input(node, &key)?;
                }
            }
            script.change(node)?;
        } else if !page.dom.set_input_text(node, value) {
            return Err("field cannot be filled");
        }
        self.settle()?;
        Ok(json!({"filled": !value.is_empty()}))
    }

    pub fn field_target(&mut self, command: &Value) -> Result<Value, &'static str> {
        let checkout_url = self
            .pages
            .first()
            .ok_or("open a document first")?
            .url
            .clone();
        let (page, node) = self.lookup(command)?;
        if !page.field_nodes()?.contains(&node) || page.origin == "null" {
            return Err("field cannot be filled");
        }
        let id = page.dom.id_attribute(node).unwrap_or_default();
        let autocomplete = page
            .dom
            .wasi_attribute(node, "autocomplete")
            .unwrap_or_default();
        if page.url.as_str().len() > 4096 || id.len() > 256 || autocomplete.len() > 80 {
            return Err("browser field metadata too large");
        }
        let target = json!({
            "checkout_url": checkout_url.as_str(), "document_url": page.url.as_str(),
            "origin": page.origin, "field_id": id, "autocomplete": autocomplete,
        });
        let document_id = page.id;
        self.check_attached(document_id)?;
        Ok(target)
    }

    fn check_attached(&self, mut id: u32) -> Result<(), &'static str> {
        while let Some(page) = self.pages.iter().find(|page| page.id == id) {
            let Some((parent_id, node)) = page.parent else {
                return Ok(());
            };
            let parent = self
                .pages
                .iter()
                .find(|parent| parent.id == parent_id)
                .ok_or("browser field document is detached")?;
            if !parent.query("iframe[src]")?.contains(&node)
                || !parent
                    .dom
                    .wasi_attribute(node, "src")
                    .is_some_and(|source| parent.url.join(&source).is_ok_and(|url| url == page.url))
            {
                return Err("browser field document changed");
            }
            id = parent_id;
        }
        Err("browser field document is detached")
    }

    pub fn match_text(&mut self, command: &Value) -> Result<Value, &'static str> {
        let selector = command
            .get("selector")
            .and_then(Value::as_str)
            .filter(|value| !value.trim().is_empty() && value.len() <= 512)
            .ok_or("invalid CSS selector")?;
        let text = command
            .get("text")
            .and_then(Value::as_str)
            .filter(|value| !value.is_empty() && value.len() <= 512)
            .ok_or("invalid text query")?;
        self.settle()?;
        let root = self.pages.first().ok_or("open a document first")?;
        let matched = root
            .query(selector)?
            .into_iter()
            .any(|node| root.dom.text_content(node).contains(text));
        Ok(json!({"matched": matched}))
    }

    pub fn click(&mut self, command: &Value) -> Result<Value, &'static str> {
        let (page, node) = self.lookup(command)?;
        if !page.action_nodes()?.contains(&node) {
            return Err("action is not available");
        }
        page.script
            .as_mut()
            .ok_or("page scripts are disabled")?
            .click(node)?;
        self.settle()?;
        Ok(json!({"clicked": true}))
    }

    fn lookup(&mut self, command: &Value) -> Result<(&mut Document, u32), &'static str> {
        let handle = command
            .get("handle")
            .and_then(Value::as_u64)
            .ok_or("invalid DOM handle")?;
        let page = self
            .pages
            .iter_mut()
            .find(|page| page.id == (handle >> 32) as u32)
            .ok_or("invalid DOM handle")?;
        Ok((page, handle as u32))
    }

    fn run(&mut self) {
        // Completed scripts run normally. A page's ordinary JS exception is a page error;
        // inspection and subsequent events can still run, as they can in a browser.
        for page in &mut self.pages {
            let _ = page.run_scripts();
        }
    }

    fn settle(&mut self) -> Result<(), &'static str> {
        for page in &mut self.pages {
            if let Some(script) = &mut page.script {
                script.tick()?;
            }
        }
        for _ in 0..MAX_TURNS {
            let mut advanced = false;
            let frames = self.unloaded_frames()?;
            for frame in frames {
                if self.pages.len() >= MAX_DOCUMENTS {
                    return Err("browser frame budget exhausted");
                }
                let parent = (frame.parent, frame.node);
                let mut page = Document::open(
                    frame.url,
                    Some(parent),
                    frame.allow_scripts,
                    frame.same_origin,
                    &self.messages,
                )?;
                let _ = page.run_scripts();
                self.pages.push(page);
                if let Some(script) = &mut self.document_mut(frame.parent)?.script {
                    script.frame_loaded(frame.node)?;
                }
                advanced = true;
            }
            let message = self.messages.borrow_mut().pop_front();
            if let Some(message) = message {
                self.deliver(message)?;
                advanced = true;
            }
            if !advanced {
                return Ok(());
            }
        }
        Err("browser event budget exhausted")
    }

    fn unloaded_frames(&self) -> Result<Vec<Frame>, &'static str> {
        let mut unloaded = Vec::new();
        for parent in &self.pages {
            for node in parent.query("iframe[src]")? {
                let source = parent.dom.wasi_attribute(node, "src").unwrap_or_default();
                let Ok(url) = parent.url.join(&source) else {
                    continue;
                };
                if !matches!(url.scheme(), "https" | "http") {
                    continue;
                }
                if let Some(old) = self
                    .pages
                    .iter()
                    .find(|p| p.parent == Some((parent.id, node)))
                {
                    if old.url != url {
                        return Err("iframe re-navigation is unavailable");
                    }
                    continue;
                }
                let sandbox = parent.dom.wasi_attribute(node, "sandbox");
                let permits = |flag| {
                    sandbox.as_ref().is_none_or(|tokens| {
                        tokens.split_ascii_whitespace().any(|token| token == flag)
                    })
                };
                unloaded.push(Frame {
                    parent: parent.id,
                    node,
                    url,
                    allow_scripts: permits("allow-scripts"),
                    same_origin: permits("allow-same-origin"),
                });
            }
        }
        Ok(unloaded)
    }

    fn deliver(&mut self, message: Message) -> Result<(), &'static str> {
        let sender = self
            .pages
            .iter()
            .find(|page| page.id == message.sender)
            .ok_or("invalid message sender")?;
        let origin = sender.origin.clone();
        let route = if message.destination.as_str() == Some("parent") {
            sender.parent.map(|(id, node)| (id, json!(node)))
        } else {
            self.pages
                .iter()
                .find(|page| {
                    page.parent.is_some_and(|(id, node)| {
                        id == sender.id && message.destination.as_u64() == Some(u64::from(node))
                    })
                })
                .map(|page| (page.id, json!("parent")))
        };
        let Some((destination, source)) = route else {
            return Ok(());
        };
        let recipient = self.document_mut(destination)?;
        let allowed = if message.target_origin == "*" {
            true
        } else if message.target_origin == "/" {
            origin != "null" && origin == recipient.origin
        } else {
            Url::parse(&message.target_origin).ok().is_some_and(|url| {
                recipient.origin != "null" && url.origin().ascii_serialization() == recipient.origin
            })
        };
        if allowed {
            if let Some(script) = &mut recipient.script {
                script.receive(&message.data, &origin, &source)?;
            }
        }
        Ok(())
    }

    fn document_mut(&mut self, id: u32) -> Result<&mut Document, &'static str> {
        self.pages
            .iter_mut()
            .find(|page| page.id == id)
            .ok_or("invalid browser document")
    }
}

struct Frame {
    parent: u32,
    node: u32,
    url: Url,
    allow_scripts: bool,
    same_origin: bool,
}

impl Document {
    fn open(
        url: Url,
        parent: Option<(u32, u32)>,
        allow_scripts: bool,
        same_origin: bool,
        messages: &Messages,
    ) -> Result<Self, &'static str> {
        let html = host::fetch(&url, None)?;
        let id = NEXT_DOCUMENT.with(|current| {
            let next = current
                .get()
                .checked_add(1)
                .ok_or("document handle space exhausted")?;
            current.set(next);
            Ok::<_, &'static str>(next)
        })?;
        let origin = if same_origin {
            url.origin().ascii_serialization()
        } else {
            "null".into()
        };
        let dom = Rc::new(DomHandle::new(&html, url.as_str()));
        let script = if allow_scripts {
            Some(PageScript::new(
                id,
                dom.clone(),
                url.clone(),
                &origin,
                parent.is_some(),
                messages.clone(),
            )?)
        } else {
            None
        };
        Ok(Self {
            id,
            url,
            origin,
            dom,
            script,
            parent,
        })
    }

    fn query(&self, selector: &str) -> Result<Vec<u32>, &'static str> {
        Ok(self
            .dom
            .query_selector_all(selector)
            .map_err(|_| "could not inspect browser DOM")?
            .into_iter()
            .map(|node| node as u32)
            .collect())
    }

    fn run_scripts(&mut self) -> Result<(), &'static str> {
        if self.script.is_none() {
            return Ok(());
        }
        let nodes = self.query("script")?;
        for node in nodes.into_iter().take(MAX_SCRIPTS_PER_DOCUMENT) {
            let kind = self.dom.wasi_attribute(node, "type").unwrap_or_default();
            if !matches!(
                kind.trim().to_ascii_lowercase().as_str(),
                "" | "text/javascript" | "application/javascript"
            ) {
                continue;
            }
            let script = if let Some(source) = self.dom.wasi_attribute(node, "src") {
                let url = self.url.join(&source).map_err(|_| "invalid script URL")?;
                host::fetch(&url, None)?
            } else {
                self.dom.text_content(node)
            };
            self.script.as_mut().unwrap().script(node, &script)?;
        }
        self.script.as_mut().unwrap().mark_ready()
    }

    fn field_nodes(&self) -> Result<Vec<u32>, &'static str> {
        Ok(self
            .query(
                "input:not([disabled]):not([readonly]), textarea:not([disabled]):not([readonly])",
            )?
            .into_iter()
            .filter(|node| {
                self.dom.wasi_attribute(*node, "type").is_none_or(|kind| {
                    matches!(
                        kind.to_ascii_lowercase().as_str(),
                        "text" | "email" | "tel" | "search" | "url" | "password" | "number"
                    )
                })
            })
            .take(MAX_FIELDS)
            .collect())
    }

    fn action_nodes(&self) -> Result<Vec<u32>, &'static str> {
        Ok(self
            .query("button:not([disabled]), input[type=submit]:not([disabled])")?
            .into_iter()
            .take(MAX_ACTIONS)
            .collect())
    }

    fn fields(&self, fields: &mut Vec<Value>) -> Result<(), &'static str> {
        let dom = &self.dom;
        let labels: HashMap<_, _> = self
            .query("label[for]")?
            .into_iter()
            .take(MAX_FIELDS)
            .filter_map(|node| Some((dom.wasi_attribute(node, "for")?, dom.text_content(node))))
            .collect();
        for node in self.field_nodes()? {
            if fields.len() >= MAX_FIELDS {
                break;
            }
            let label = dom
                .wasi_attribute(node, "aria-label")
                .or_else(|| labels.get(&dom.id_attribute(node)?).cloned())
                .or_else(|| dom.wasi_attribute(node, "placeholder"))
                .unwrap_or_default();
            fields.push(json!({
                "handle": self.handle(node), "label": bounded(&label, 200),
                "autocomplete": bounded(&dom.wasi_attribute(node, "autocomplete").unwrap_or_default(), 80),
                "filled": dom.wasi_filled(node),
                "origin": self.origin,
            }));
        }
        Ok(())
    }

    fn actions(&self, actions: &mut Vec<Value>) -> Result<(), &'static str> {
        for node in self.action_nodes()? {
            if actions.len() >= MAX_ACTIONS {
                break;
            }
            let label = self
                .dom
                .wasi_attribute(node, "aria-label")
                .or_else(|| self.dom.wasi_attribute(node, "value"))
                .unwrap_or_else(|| self.dom.text_content(node));
            actions
                .push(json!({ "handle": self.handle(node), "label": bounded(label.trim(), 200) }));
        }
        Ok(())
    }

    fn handle(&self, node: u32) -> u64 {
        (u64::from(self.id) << 32) | u64::from(node)
    }
}

fn bounded(text: &str, chars: usize) -> String {
    text.chars().take(chars).collect()
}
