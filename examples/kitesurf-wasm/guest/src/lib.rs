use kitesurf_core::DomHandle;
use serde_json::{json, Value};
use std::cell::{Cell, RefCell};
use std::collections::HashMap;
use std::io::{self, Read, Write};

const MAX_DOCUMENT_BYTES: usize = 1024 * 1024;
const MAX_FIELDS: usize = 100;

struct Page {
    dom: DomHandle,
    generation: u32,
    fields: Vec<u32>,
}

thread_local! {
    static GENERATION: Cell<u32> = const { Cell::new(0) };
    static PAGE: RefCell<Option<Page>> = const { RefCell::new(None) };
}

#[link(wasm_import_module = "vgs_browser_v1")]
extern "C" {
    fn http_get(url: *const u8, url_size: usize, output: *mut u8, capacity: usize) -> i32;
}

#[no_mangle]
pub extern "C" fn _initialize() {}

#[no_mangle]
pub extern "C" fn vgs_browser_step() {
    let result = read_command().and_then(execute);
    let reply = result.unwrap_or_else(|error| json!({"error": error}));
    let mut stdout = io::stdout().lock();
    let _ = stdout.write_all(reply.to_string().as_bytes());
    let _ = stdout.flush();
}

fn read_command() -> Result<Value, &'static str> {
    let mut input = Vec::new();
    io::stdin()
        .take(16385)
        .read_to_end(&mut input)
        .map_err(|_| "could not read the browser command")?;
    if input.len() > 16384 {
        return Err("browser command is too large");
    }
    serde_json::from_slice(&input).map_err(|_| "invalid browser command")
}

fn execute(command: Value) -> Result<Value, &'static str> {
    match command.get("op").and_then(Value::as_str) {
        Some("open") => {
            PAGE.with(|page| page.replace(None));
            let url = command.get("url").and_then(Value::as_str).ok_or("missing document URL")?;
            let page = open(url)?;
            PAGE.with(|slot| slot.replace(Some(page)));
            Ok(json!({"opened": true}))
        }
        Some("inspect") => PAGE.with(|page| {
            let page = page.borrow();
            inspect(page.as_ref().ok_or("open a document first")?)
        }),
        Some("fill") => PAGE.with(|page| {
            let mut page = page.borrow_mut();
            fill(page.as_mut().ok_or("open a document first")?, &command)
        }),
        _ => Err("unknown browser command"),
    }
}

fn open(url: &str) -> Result<Page, &'static str> {
    if url.is_empty() || url.len() > 4096 {
        return Err("invalid document URL");
    }
    let mut html = vec![0; MAX_DOCUMENT_BYTES];
    // The host checks that both byte ranges are in the current guest memory.
    let size = unsafe { http_get(url.as_ptr(), url.len(), html.as_mut_ptr(), html.len()) };
    if size < 0 || size as usize > html.len() {
        return Err("document fetch failed");
    }
    let html = std::str::from_utf8(&html[..size as usize]).map_err(|_| "invalid document encoding")?;
    let dom = DomHandle::new(html, url);
    let fields = dom.query_selector_all(
        "input:not([disabled]):not([readonly]), textarea:not([disabled]):not([readonly])"
    ).map_err(|_| "could not inspect the document")?
        .into_iter().map(|node| node as u32).filter(|node| {
            match dom.wasi_attribute(*node, "type") {
                None => true,
                Some(kind) => matches!(kind.to_ascii_lowercase().as_str(),
                    "text" | "email" | "tel" | "search" | "url" | "password" | "number")
            }
        }).take(MAX_FIELDS).collect();
    let generation = GENERATION.with(|current| {
        let next = current.get().checked_add(1).ok_or("document handle space exhausted")?;
        current.set(next);
        Ok::<_, &'static str>(next)
    })?;
    Ok(Page { dom, generation, fields })
}

fn inspect(page: &Page) -> Result<Value, &'static str> {
    let dom = &page.dom;
    let select = |query| dom.query_selector_all(query).map_err(|_| "could not inspect the document");
    let title = select("title")?.first().map(|node| dom.text_content(*node as u32)).unwrap_or_default();
    let labels: HashMap<_, _> = select("label[for]")?.into_iter().take(MAX_FIELDS)
        .filter_map(|node| {
            let node = node as u32;
            Some((dom.wasi_attribute(node, "for")?, dom.text_content(node)))
        }).collect();
    let fields: Vec<_> = page.fields.iter().map(|&node| {
        let label = dom.wasi_attribute(node, "aria-label")
            .or_else(|| labels.get(&dom.id_attribute(node)?).cloned())
            .or_else(|| dom.wasi_attribute(node, "placeholder"))
            .unwrap_or_default();
        json!({
            "handle": (u64::from(page.generation) << 32) | u64::from(node),
            "label": label.chars().take(200).collect::<String>(),
            "autocomplete": dom.wasi_attribute(node, "autocomplete")
                .unwrap_or_default().chars().take(80).collect::<String>(),
            "filled": dom.wasi_filled(node),
        })
    }).collect();
    Ok(json!({
        "title": title.chars().take(200).collect::<String>(),
        "script_count": select("script")?.len(),
        "iframe_count": select("iframe")?.len(),
        "fields": fields,
    }))
}

fn fill(page: &mut Page, command: &Value) -> Result<Value, &'static str> {
    let handle = command.get("handle").and_then(Value::as_u64)
        .filter(|handle| (handle >> 32) == u64::from(page.generation))
        .ok_or("invalid field handle")? as u32;
    let value = command.get("value").and_then(Value::as_str)
        .filter(|value| value.len() <= 1024)
        .ok_or("invalid field value")?;
    if !page.fields.contains(&handle) || !page.dom.set_input_text(handle, value) {
        return Err("field cannot be filled");
    }
    Ok(json!({"filled": !value.is_empty()}))
}
