// Persistent page JavaScript with an origin-scoped Kitesurf DOM.
use crate::host;
use boa_engine::{
    context::{Clock, time::JsInstant}, js_string, property::Attribute, Context, JsNativeError,
    JsObject, JsResult, JsString, JsValue, NativeFunction, Source,
};
use kitesurf_core::DomHandle;
use serde_json::{json, Value};
use std::cell::{Cell, RefCell};
use std::collections::VecDeque;
use std::rc::Rc;
use url::Url;

pub type Messages = Rc<RefCell<VecDeque<Message>>>;
const MAX_PENDING_MESSAGES: usize = 256;
const MAX_MESSAGE_BYTES: usize = 128 * 1024;

pub struct Message {
    pub sender: u32,
    pub destination: Value,
    pub data: Value,
    pub target_origin: String,
}

struct PageData {
    id: u32,
    dom: Rc<DomHandle>,
    url: Url,
    origin: String,
    messages: Messages,
}

pub struct PageScript {
    ctx: Context,
    ready: JsObject,
    input: JsObject,
    change: JsObject,
    keydown: JsObject,
    inbound: JsObject,
    frame_loaded: JsObject,
    tick: JsObject,
    click: JsObject,
    current_script: JsObject,
}

impl PageScript {
    pub fn new(id: u32, dom: Rc<DomHandle>, url: Url, origin: &str,
        has_parent: bool, messages: Messages) -> Result<Self, &'static str> {
        let mut ctx = Context::builder().clock(Rc::new(PageClock::default())).build()
            .map_err(|_| "could not initialize browser JavaScript")?;
        ctx.insert_data(PageData { id, dom, url: url.clone(), origin: origin.to_string(), messages });
        let info = json!({
            "url": url.as_str(),
            "origin": origin,
            "hasParent": has_parent,
            "readyState": "loading",
        });
        ctx.register_global_property(js_string!("__browserPageInfo"),
            JsString::from(info.to_string().as_str()), Attribute::all())
            .map_err(|_| "could not bind browser page information")?;
        ctx.register_global_callable(js_string!("__browserNative"), 2,
            NativeFunction::from_fn_ptr(browser_native))
            .map_err(|_| "could not bind browser DOM")?;
        ctx.eval(Source::from_bytes(include_str!("web_crypto.js")))
            .map_err(|_| "could not install WebCrypto")?;
        ctx.eval(Source::from_bytes(include_str!("page.js")))
            .map_err(|_| "could not install the page APIs")?;
        let ready = take_hook(&mut ctx, "__browserReady")?;
        let input = take_hook(&mut ctx, "__browserInput")?;
        let change = take_hook(&mut ctx, "__browserChange")?;
        let keydown = take_hook(&mut ctx, "__browserKeydown")?;
        let inbound = take_hook(&mut ctx, "__browserMessage")?;
        let frame_loaded = take_hook(&mut ctx, "__browserFrameLoaded")?;
        let tick = take_hook(&mut ctx, "__browserPumpTimers")?;
        let click = take_hook(&mut ctx, "__browserClick")?;
        let current_script = take_hook(&mut ctx, "__browserCurrentScript")?;
        let global = ctx.global_object();
        global.delete_property_or_throw(js_string!("__browserNative"), &mut ctx)
            .map_err(|_| "could not seal the browser bridge")?;
        global.delete_property_or_throw(js_string!("__browserPageInfo"), &mut ctx)
            .map_err(|_| "could not seal browser page information")?;
        Ok(Self { ctx, ready, input, change, keydown, inbound, frame_loaded, tick, click, current_script })
    }

    pub fn script(&mut self, node: u32, source: &str) -> Result<(), &'static str> {
        call(&mut self.ctx, &self.current_script, &[JsValue::from(node)])?;
        let evaluated = self.ctx.eval(Source::from_bytes(source.as_bytes()));
        call(&mut self.ctx, &self.current_script, &[JsValue::null()])?;
        evaluated.map_err(|_| "page JavaScript threw an exception")?;
        self.drain_jobs()
    }

    pub fn mark_ready(&mut self) -> Result<(), &'static str> {
        call(&mut self.ctx, &self.ready, &[])?;
        self.drain_jobs()
    }

    pub fn frame_loaded(&mut self, node: u32) -> Result<(), &'static str> {
        call(&mut self.ctx, &self.frame_loaded, &[JsValue::from(node)])?;
        self.drain_jobs()
    }

    pub fn tick(&mut self) -> Result<(), &'static str> {
        call(&mut self.ctx, &self.tick, &[])?;
        self.drain_jobs()
    }

    pub fn input(&mut self, node: u32, text: &str) -> Result<(), &'static str> {
        call(&mut self.ctx, &self.input, &[JsValue::from(node), JsString::from(text).into()])?;
        self.drain_jobs()
    }

    pub fn change(&mut self, node: u32) -> Result<(), &'static str> {
        call(&mut self.ctx, &self.change, &[JsValue::from(node)])?;
        self.drain_jobs()
    }

    pub fn keydown(&mut self, node: u32, character: &str) -> Result<bool, &'static str> {
        self.keydown.call(&JsValue::undefined(),
            &[JsValue::from(node), JsString::from(character).into()], &mut self.ctx)
            .map(|value| value.to_boolean()).map_err(|_| "browser key handler failed")
    }

    pub fn click(&mut self, node: u32) -> Result<(), &'static str> {
        call(&mut self.ctx, &self.click, &[JsValue::from(node)])?;
        self.drain_jobs()
    }

    pub fn receive(&mut self, data: &Value, origin: &str, source: &Value)
        -> Result<(), &'static str> {
        let data = JsValue::from_json(data, &mut self.ctx)
            .map_err(|_| "invalid browser message")?;
        let source = JsValue::from_json(source, &mut self.ctx)
            .map_err(|_| "invalid browser message source")?;
        call(&mut self.ctx, &self.inbound, &[data, JsString::from(origin).into(), source])?;
        self.drain_jobs()
    }

    fn drain_jobs(&mut self) -> Result<(), &'static str> {
        self.ctx.run_jobs().map_err(|_| "browser JavaScript job failed")
    }
}

fn take_hook(ctx: &mut Context, name: &str) -> Result<JsObject, &'static str> {
    let key = JsString::from(name);
    let object = ctx.global_object();
    let function = object.get(key.clone(), ctx).map_err(|_| "could not install a page hook")?
        .as_object().filter(|value| value.is_callable()).ok_or("missing page hook")?;
    object.delete_property_or_throw(key, ctx).map_err(|_| "could not seal a page hook")?;
    Ok(function)
}

fn call(ctx: &mut Context, function: &JsObject, args: &[JsValue]) -> Result<(), &'static str> {
    function.call(&JsValue::undefined(), args, ctx).map(|_| ())
        .map_err(|_| "browser event handler failed")
}

#[derive(Default)]
struct PageClock(Cell<i64>);

impl Clock for PageClock {
    fn now(&self) -> JsInstant {
        let millis = self.0.get().max(host::clock_ms());
        self.0.set(millis);
        JsInstant::new((millis / 1000) as u64, ((millis % 1000) * 1_000_000) as u32)
    }

    fn system_time_millis(&self) -> i64 {
        host::clock_ms()
    }
}

fn browser_native(_: &JsValue, args: &[JsValue], ctx: &mut Context) -> JsResult<JsValue> {
    let operation = args.first().ok_or_else(|| JsNativeError::typ().with_message("missing operation"))?
        .to_string(ctx)?.to_std_string_escaped();
    let argument = args.get(1).ok_or_else(|| JsNativeError::typ().with_message("missing arguments"))?
        .to_string(ctx)?.to_std_string_escaped();
    if argument.len() > 8 * 1024 * 1024 {
        return Err(JsNativeError::range().with_message("browser command is too large").into());
    }
    let args: Vec<Value> = serde_json::from_str(&argument)
        .map_err(|_| JsNativeError::typ().with_message("invalid browser arguments"))?;
    let page = ctx.get_data::<PageData>()
        .ok_or_else(|| JsNativeError::typ().with_message("missing browser page"))?;
    let response = dispatch(page, &operation, &args).map_err(|message|
        JsNativeError::typ().with_message(message))?;
    Ok(JsString::from(response.to_string().as_str()).into())
}

fn dispatch(page: &PageData, op: &str, args: &[Value]) -> Result<Value, &'static str> {
    let dom = &page.dom;
    let node = |index| args.get(index).and_then(Value::as_u64)
        .and_then(|value| u32::try_from(value).ok()).ok_or("invalid DOM node");
    let text = |index| args.get(index).and_then(Value::as_str).ok_or("invalid DOM string");
    Ok(match op {
        "root" => json!(dom.document_node_id()),
        "query" => {
            let result = if args.get(1).is_some_and(|value| !value.is_null()) {
                dom.query_selector_all_in_subtree(node(1)?, text(0)?)
            } else {
                dom.query_selector_all(text(0)?)
            };
            json!(result.map_err(|_| "invalid CSS selector")?)
        }
        "info" => dom.wasi_node_info(node(0)?).unwrap_or(Value::Null),
        "getAttr" => json!(dom.wasi_attribute(node(0)?, text(1)?)),
        "setAttr" => json!(dom.set_attribute(node(0)?, text(1)?, text(2)?)),
        "removeAttr" => json!(dom.remove_attribute(node(0)?, text(1)?)),
        "getText" => json!(dom.text_content(node(0)?)),
        "setText" => json!(dom.set_node_text(node(0)?, text(1)?)),
        "getHtml" => json!(dom.inner_html(node(0)?)),
        "setHtml" => json!(dom.wasi_set_html(node(0)?, text(1)?)),
        "getValue" => json!(dom.wasi_input_value(node(0)?).unwrap_or_default()),
        "setValue" => json!(dom.set_input_text(node(0)?, text(1)?)),
        "create" => {
            let data = text(1)?;
            json!(match text(0)? {
                "element" => dom.create_element(data),
                "text" => dom.create_text_node(data),
                "comment" => dom.create_comment(data),
                _ => return Err("unsupported node type"),
            })
        }
        "append" => json!(dom.append_child_node(node(0)?, node(1)?)),
        "insert" if args.get(2).is_some_and(|value| !value.is_null()) =>
            json!(dom.insert_before_node(node(0)?, node(1)?, node(2)?)),
        "insert" => json!(dom.append_child_node(node(0)?, node(1)?)),
        "remove" => {
            let child = node(1)?;
            let matching_parent = dom.wasi_node_info(child).and_then(|value|
                value["parent"].as_u64()).is_some_and(|parent| Ok(parent as u32) == node(0));
            json!(matching_parent && dom.remove_node(child))
        }
        "focus" => json!(dom.set_focus(node(0)?)),
        "setSelection" => json!(dom.set_input_selection(node(0)?, node(1)?, node(2)?)),
        "rect" => Value::Null,
        "resolveUrl" => {
            let base = args.get(1).and_then(Value::as_str).filter(|base| !base.is_empty())
                .map(Url::parse).transpose().map_err(|_| "invalid base URL")?;
            let url = base.as_ref().unwrap_or(&page.url).join(text(0)?)
                .map_err(|_| "invalid page URL")?;
            url_info(&url)
        }
        "fetch" => {
            let url = page.url.join(text(0)?).map_err(|_| "invalid resource URL")?;
            if page.origin == "null" || url.origin().ascii_serialization() != page.origin {
                return Err("cross-origin fetch unavailable");
            }
            let body = args.get(1).and_then(Value::as_str);
            json!(host::fetch(&url, body)?)
        }
        "send" => {
            let [destination, data, target_origin] = args else {
                return Err("invalid browser message");
            };
            let target_origin = target_origin.as_str().ok_or("invalid message origin")?;
            if data.to_string().len() > MAX_MESSAGE_BYTES || page.messages.borrow().len() >= MAX_PENDING_MESSAGES {
                return Err("browser message budget exhausted");
            }
            page.messages.borrow_mut().push_back(Message {
                sender: page.id, destination: destination.clone(), data: data.clone(),
                target_origin: target_origin.to_string(),
            });
            Value::Null
        }
        "random" => host::random(node(0)? as usize)?,
        "aesGcm" => host::encrypt_aes(args)?,
        "rsaOaep256" => host::encrypt_rsa(args)?,
        _ => return Err("unsupported browser operation"),
    })
}

fn url_info(url: &Url) -> Value {
    let hostname = url.host_str().unwrap_or_default();
    let port = url.port().map(|port| port.to_string()).unwrap_or_default();
    let host = if port.is_empty() { hostname.to_string() } else { format!("{hostname}:{port}") };
    json!({
        "href": url.as_str(), "origin": url.origin().ascii_serialization(),
        "protocol": format!("{}:", url.scheme()), "hostname": hostname, "host": host,
        "port": port, "pathname": url.path(),
        "search": url.query().map(|value| format!("?{value}")).unwrap_or_default(),
        "hash": url.fragment().map(|value| format!("#{value}")).unwrap_or_default(),
    })
}
