from typing import (
    Any, Iterable, Mapping, Optional, Sequence, Tuple, Type, Union,
)

from wsgiref.types import WSGIEnvironment

from .datastructures import (
    CombinedMultiDict, EnvironHeaders, Headers, ImmutableMultiDict,
    MultiDict, TypeConversionDict,
)

class BaseRequest:
    charset = ...  # type: str
    encoding_errors = ...  # type: str
    max_content_length = ...  # type: int
    max_form_memory_size = ...  # type: int
    parameter_storage_class = ...  # type: Type
    list_storage_class = ...  # type: Type
    dict_storage_class = ...  # type: Type
    form_data_parser_class = ...  # type: Type
    trusted_hosts = ...  # type: Optional[Sequence[str]]
    disable_data_descriptor = ...  # type: Any
    environ: WSGIEnvironment = ...
    shallow = ...  # type: Any
    def __init__(self, environ: WSGIEnvironment, populate_request: bool = ..., shallow: bool = ...) -> None: ...
    @property
    def url_charset(self) -> str: ...
    @classmethod
    def from_values(cls, *args, **kwargs) -> 'BaseRequest': ...
    @classmethod
    def application(cls, f): ...
    @property
    def want_form_data_parsed(self): ...
    def make_form_data_parser(self): ...
    def close(self) -> None: ...
    def __enter__(self): ...
    def __exit__(self, exc_type, exc_value, tb): ...
    def stream(self): ...
    input_stream = ...  # type: Any
    args = ...  # type: ImmutableMultiDict
    @property
    def data(self) -> bytes: ...
    def get_data(self, cache: bool = ..., as_text: bool = ..., parse_form_data: bool = ...) -> bytes: ...
    form = ...  # type: ImmutableMultiDict
    values = ...  # type: CombinedMultiDict
    files = ...  # type: MultiDict
    cookies = ...  # type: TypeConversionDict
    headers = ...  # type: EnvironHeaders
    path = ...  # type: str
    full_path = ...  # type: str
    script_root = ...  # type: str
    url = ...  # type: str
    base_url = ...  # type: str
    url_root = ...  # type: str
    host_url = ...  # type: str
    host = ...  # type: str
    query_string = ...  # type: bytes
    method = ...  # type: str
    def access_route(self): ...
    @property
    def remote_addr(self) -> str: ...
    remote_user = ...  # type: str
    scheme = ...  # type: str
    is_xhr = ...  # type: bool
    is_secure = ...  # type: bool
    is_multithread = ...  # type: bool
    is_multiprocess = ...  # type: bool
    is_run_once = ...  # type: bool

class BaseResponse:
    charset = ...  # type: str
    default_status = ...  # type: int
    default_mimetype = ...  # type: str
    implicit_sequence_conversion = ...  # type: bool
    autocorrect_location_header = ...  # type: bool
    automatically_set_content_length = ...  # type: bool
    headers = ...  # type: Headers
    status_code = ...  # type: int
    status = ...  # type: str
    direct_passthrough = ...  # type: bool
    response = ...  # type: Iterable[bytes]
    def __init__(self, response: Optional[Union[Iterable[bytes], bytes]] = ...,
                 status: Optional[Union[str, int]] = ...,
                 headers: Optional[Union[Headers,
                                         Mapping[str, str],
                                         Sequence[Tuple[str, str]]]]=None,
                 mimetype: Optional[str] = ...,
                 content_type: Optional[str] = ...,
                 direct_passthrough: bool = ...) -> None: ...
    def call_on_close(self, func): ...
    @classmethod
    def force_type(cls, response, environ=None): ...
    @classmethod
    def from_app(cls, app, environ, buffered=False): ...
    def get_data(self, as_text=False): ...
    def set_data(self, value): ...
    data = ...  # type: Any
    def calculate_content_length(self): ...
    def make_sequence(self): ...
    def iter_encoded(self): ...
    def set_cookie(self, key, value='', max_age=None, expires=None, path='', domain=None, secure=False, httponly=False): ...
    def delete_cookie(self, key, path='', domain=None): ...
    @property
    def is_streamed(self) -> bool: ...
    @property
    def is_sequence(self) -> bool: ...
    def close(self) -> None: ...
    def __enter__(self): ...
    def __exit__(self, exc_type, exc_value, tb): ...
    def freeze(self, **kwargs): ...
    def get_wsgi_headers(self, environ): ...
    def get_app_iter(self, environ): ...
    def get_wsgi_response(self, environ): ...
    def __call__(self, environ, start_response): ...

class AcceptMixin:
    def accept_mimetypes(self): ...
    def accept_charsets(self): ...
    def accept_encodings(self): ...
    def accept_languages(self): ...

class ETagRequestMixin:
    def cache_control(self): ...
    def if_match(self): ...
    def if_none_match(self): ...
    def if_modified_since(self): ...
    def if_unmodified_since(self): ...
    def if_range(self): ...
    def range(self): ...

class UserAgentMixin:
    def user_agent(self): ...

class AuthorizationMixin:
    def authorization(self): ...

class StreamOnlyMixin:
    disable_data_descriptor = ...  # type: Any
    want_form_data_parsed = ...  # type: Any

class ETagResponseMixin:
    @property
    def cache_control(self): ...
    status_code = ...  # type: Any
    def make_conditional(self, request_or_environ, accept_ranges=False, complete_length=None): ...
    def add_etag(self, overwrite=False, weak=False): ...
    def set_etag(self, etag, weak=False): ...
    def get_etag(self): ...
    def freeze(self, *, no_etag=False): ...
    accept_ranges = ...  # type: Any
    content_range = ...  # type: Any

class ResponseStream:
    mode = ...  # type: Any
    response = ...  # type: Any
    closed = ...  # type: Any
    def __init__(self, response): ...
    def write(self, value): ...
    def writelines(self, seq): ...
    def close(self): ...
    def flush(self): ...
    def isatty(self): ...
    @property
    def encoding(self): ...

class ResponseStreamMixin:
    def stream(self): ...

class CommonRequestDescriptorsMixin:
    content_type = ...  # type: Any
    def content_length(self): ...
    content_encoding = ...  # type: Any
    content_md5 = ...  # type: Any
    referrer = ...  # type: Any
    date = ...  # type: Any
    max_forwards = ...  # type: Any
    @property
    def mimetype(self): ...
    @property
    def mimetype_params(self): ...
    def pragma(self): ...

class CommonResponseDescriptorsMixin:
    mimetype = ...  # type: Any
    mimetype_params = ...  # type: Any
    location = ...  # type: Any
    age = ...  # type: Any
    content_type = ...  # type: Any
    content_length = ...  # type: Any
    content_location = ...  # type: Any
    content_encoding = ...  # type: Any
    content_md5 = ...  # type: Any
    date = ...  # type: Any
    expires = ...  # type: Any
    last_modified = ...  # type: Any
    retry_after = ...  # type: Any
    vary = ...  # type: Any
    content_language = ...  # type: Any
    allow = ...  # type: Any

class WWWAuthenticateMixin:
    @property
    def www_authenticate(self): ...

class Request(BaseRequest, AcceptMixin, ETagRequestMixin, UserAgentMixin, AuthorizationMixin, CommonRequestDescriptorsMixin): ...
class PlainRequest(StreamOnlyMixin, Request): ...
class Response(BaseResponse, ETagResponseMixin, ResponseStreamMixin, CommonResponseDescriptorsMixin, WWWAuthenticateMixin): ...
