# Copyright 2017 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


load("types", "types")

# def _check_dict_like(d, check_keys=True, check_values=True, check_getitem=True, name='val', return_as_bool=False):
#     """Helper to check if given val has various dict-like attributes."""
#     if not isinstance(d, Iterable):
#         if return_as_bool:
#             return False
#         else:
#             raise TypeError('%s <%s> is not dict-like: not iterable' % (name, type(d).__name__))
#     if check_keys:
#         if not hasattr(d, 'keys') or not callable(getattr(d, 'keys')):
#             if return_as_bool:
#                 return False
#             else:
#                 raise TypeError('%s <%s> is not dict-like: missing keys()' % (name, type(d).__name__))
#     if check_values:
#         if not hasattr(d, 'values') or not callable(getattr(d, 'values')):
#             if return_as_bool:
#                 return False
#             else:
#                 raise TypeError('%s <%s> is not dict-like: missing values()' % (name, type(d).__name__))
#     if check_getitem:
#         if not hasattr(d, '__getitem__'):
#             if return_as_bool:
#                 return False
#             else:
#                 raise TypeError('%s <%s> is not dict-like: missing [] accessor' % (name, type(d).__name__))
#     if return_as_bool:
#         return True
#
# def _check_iterable(self, l, check_getitem=True, name='val'):
#     """Helper to check if given val has various iterable attributes."""
#     if not isinstance(l, Iterable):
#         raise TypeError('%s <%s> is not iterable' % (name, type(l).__name__))
#     if check_getitem:
#         if not hasattr(l, '__getitem__'):
#             raise TypeError('%s <%s> does not have [] accessor' % (name, type(l).__name__))
#
# def _dict_not_equal(self, val, other, ignore=None, include=None):
#     """Helper to compare dicts."""
#     if ignore or include:
#         ignores = self._dict_ignore(ignore)
#         includes = self._dict_include(include)
#
#         # guarantee include keys are in val
#         if include:
#             missing = []
#             for i in includes:
#                 if i not in val:
#                     missing.append(i)
#             if missing:
#                 self.error('Expected <%s> to include key%s %s, but did not include key%s %s.' % (
#                     val,
#                     '' if len(includes) == 1 else 's',
#                     self._fmt_items(['.'.join([str(s) for s in i]) if type(i) is tuple else i for i in includes]),
#                     '' if len(missing) == 1 else 's',
#                     self._fmt_items(missing)))
#
#         # calc val keys given ignores and includes
#         if ignore and include:
#             k1 = set([k for k in val if k not in ignores and k in includes])
#         elif ignore:
#             k1 = set([k for k in val if k not in ignores])
#         else:  # include
#             k1 = set([k for k in val if k in includes])
#
#         # calc other keys given ignores and includes
#         if ignore and include:
#             k2 = set([k for k in other if k not in ignores and k in includes])
#         elif ignore:
#             k2 = set([k for k in other if k not in ignores])
#         else:  # include
#             k2 = set([k for k in other if k in includes])
#
#         if k1 != k2:
#             # different set of keys, so not equal
#             return True
#         else:
#             for k in k1:
#                 if self._check_dict_like(val[k], check_values=False, return_as_bool=True) and \
#                         self._check_dict_like(other[k], check_values=False, return_as_bool=True):
#                     subdicts_not_equal = self._dict_not_equal(
#                         val[k],
#                         other[k],
#                         ignore=[i[1:] for i in ignores if type(i) is tuple and i[0] == k] if ignore else None,
#                         include=[i[1:] for i in self._dict_ignore(include) if type(i) is tuple and i[0] == k] if include else None)
#                     if subdicts_not_equal:
#                         # fast fail inside the loop since sub-dicts are not equal
#                         return True
#                 elif val[k] != other[k]:
#                     # fast fail inside the loop since values are not equal
#                     return True
#         return False
#     else:
#         return val != other
#
# def _dict_ignore(self, ignore):
#     """Helper to make list for given ignore kwarg values."""
#     return [i[0] if type(i) is tuple and len(i) == 1 else i for i in (ignore if type(ignore) is list else [ignore])]
#
# def _dict_include(self, include):
#     """Helper to make a list from given include kwarg values."""
#     return [i[0] if type(i) is tuple else i for i in (include if type(include) is list else [include])]
#
# def _dict_err(self, val, other, ignore=None, include=None):
#     """Helper to construct error message for dict comparison."""
#     def _dict_repr(d, other):
#         out = ''
#         ellip = False
#         for k, v in sorted(d.items()):
#             if k not in other:
#                 out += '%s%s: %s' % (', ' if len(out) > 0 else '', repr(k), repr(v))
#             elif v != other[k]:
#                 out += '%s%s: %s' % (
#                     ', ' if len(out) > 0 else '',
#                     repr(k),
#                     _dict_repr(v, other[k]) if self._check_dict_like(
#                         v, check_values=False, return_as_bool=True) and self._check_dict_like(
#                             other[k], check_values=False, return_as_bool=True) else repr(v)
#                 )
#             else:
#                 ellip = True
#         return '{%s%s}' % ('..' if ellip and len(out) == 0 else '.., ' if ellip else '', out)
#
#     if ignore:
#         ignores = self._dict_ignore(ignore)
#         ignore_err = ' ignoring keys %s' % self._fmt_items(['.'.join([str(s) for s in i]) if type(i) is tuple else i for i in ignores])
#     if include:
#         includes = self._dict_ignore(include)
#         include_err = ' including keys %s' % self._fmt_items(['.'.join([str(s) for s in i]) if type(i) is tuple else i for i in includes])
#
#     self.error('Expected <%s> to be equal to <%s>%s%s, but was not.' % (
#         _dict_repr(val, other),
#         _dict_repr(other, val),
#         ignore_err if ignore else '',
#         include_err if include else ''
#     ))



def _add(*dictionaries, **kwargs):
    """Returns a new `dict` that has all the entries of the given dictionaries.

    If the same key is present in more than one of the input dictionaries, the
    last of them in the argument list overrides any earlier ones.

    This function is designed to take zero or one arguments as well as multiple
    dictionaries, so that it follows arithmetic identities and callers can avoid
    special cases for their inputs: the sum of zero dictionaries is the empty
    dictionary, and the sum of a single dictionary is a copy of itself.

    Args:
      *dictionaries: Zero or more dictionaries to be added.
      **kwargs: Additional dictionary passed as keyword args.

    Returns:
      A new `dict` that has all the entries of the given dictionaries.
    """
    result = {}
    for d in dictionaries:
        result.update(d)
    result.update(kwargs)
    return result

dicts = struct(
    add = _add,
)
