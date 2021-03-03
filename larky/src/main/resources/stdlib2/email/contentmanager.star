def ContentManager:
    """
    ''
    """
    def add_set_handler(self, typekey, handler):
        """
        'multipart'
        """
    def _find_set_handler(self, msg, obj):
        """
        '__module__'
        """
def get_text_content(msg, errors='replace'):
    """
    'charset'
    """
def get_non_text_content(msg):
    """
    'audio image video application'
    """
def get_message_content(msg):
    """
    'rfc822 external-body'
    """
def get_and_fixup_unknown_message_content(msg):
    """
     If we don't understand a message subtype, we are supposed to treat it as
     if it were application/octet-stream, per
     tools.ietf.org/html/rfc2046#section-5.2.4.  Feedparser doesn't do that,
     so do our best to fix things up.  Note that it is *not* appropriate to
     model message/partial content as Message objects, so they are handled
     here as well.  (How to reassemble them is out of scope for this comment :)

    """
def _prepare_set(msg, maintype, subtype, headers):
    """
    'Content-Type'
    """
def _finalize_set(msg, disposition, filename, cid, params):
    """
    'attachment'
    """
def _encode_base64(data, max_line_length):
    """
    'ascii'
    """
def _encode_text(string, charset, cte, policy):
    """
    'ascii'
    """
    def embedded_body(lines): return linesep.join(lines) + linesep
        """
        b'\n'
        """
2021-03-02 20:54:36,790 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:36,791 : INFO : tokenize_signature : --> do i ever get here?
def set_text_content(msg, string, subtype="plain", charset='utf-8', cte=None,
                     disposition=None, filename=None, cid=None,
                     params=None, headers=None):
    """
    'text'
    """
2021-03-02 20:54:36,791 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:36,791 : INFO : tokenize_signature : --> do i ever get here?
def set_message_content(msg, message, subtype="rfc822", cte=None,
                       disposition=None, filename=None, cid=None,
                       params=None, headers=None):
    """
    'partial'
    """
2021-03-02 20:54:36,792 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:36,792 : INFO : tokenize_signature : --> do i ever get here?
def set_bytes_content(msg, data, maintype, subtype, cte='base64',
                     disposition=None, filename=None, cid=None,
                     params=None, headers=None):
    """
    'base64'
    """
