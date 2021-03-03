def AddressValueError(ValueError):
    """
    A Value Error related to the address.
    """
def NetmaskValueError(ValueError):
    """
    A Value Error related to the netmask.
    """
def ip_address(address):
    """
    Take an IP string/int and return an object of the correct type.

        Args:
            address: A string or integer, the IP address.  Either IPv4 or
              IPv6 addresses may be supplied; integers less than 2**32 will
              be considered to be IPv4 by default.

        Returns:
            An IPv4Address or IPv6Address object.

        Raises:
            ValueError: if the *address* passed isn't either a v4 or a v6
              address

    
    """
def ip_network(address, strict=True):
    """
    Take an IP string/int and return an object of the correct type.

        Args:
            address: A string or integer, the IP network.  Either IPv4 or
              IPv6 networks may be supplied; integers less than 2**32 will
              be considered to be IPv4 by default.

        Returns:
            An IPv4Network or IPv6Network object.

        Raises:
            ValueError: if the string passed isn't either a v4 or a v6
              address. Or if the network has host bits set.

    
    """
def ip_interface(address):
    """
    Take an IP string/int and return an object of the correct type.

        Args:
            address: A string or integer, the IP address.  Either IPv4 or
              IPv6 addresses may be supplied; integers less than 2**32 will
              be considered to be IPv4 by default.

        Returns:
            An IPv4Interface or IPv6Interface object.

        Raises:
            ValueError: if the string passed isn't either a v4 or a v6
              address.

        Notes:
            The IPv?Interface classes describe an Address on a particular
            Network, so they're basically a combination of both the Address
            and Network classes.

    
    """
def v4_int_to_packed(address):
    """
    Represent an address as 4 packed bytes in network (big-endian) order.

        Args:
            address: An integer representation of an IPv4 IP address.

        Returns:
            The integer address packed as 4 bytes in network (big-endian) order.

        Raises:
            ValueError: If the integer is negative or too large to be an
              IPv4 IP address.

    
    """
def v6_int_to_packed(address):
    """
    Represent an address as 16 packed bytes in network (big-endian) order.

        Args:
            address: An integer representation of an IPv6 IP address.

        Returns:
            The integer address packed as 16 bytes in network (big-endian) order.

    
    """
def _split_optional_netmask(address):
    """
    Helper to split the netmask and raise AddressValueError if needed
    """
def _find_address_range(addresses):
    """
    Find a sequence of sorted deduplicated IPv#Address.

        Args:
            addresses: a list of IPv#Address objects.

        Yields:
            A tuple containing the first and last IP addresses in the sequence.

    
    """
def _count_righthand_zero_bits(number, bits):
    """
    Count the number of zero bits on the right hand side.

        Args:
            number: an integer.
            bits: maximum number of bits to count.

        Returns:
            The number of zero bits on the right hand side of the number.

    
    """
def summarize_address_range(first, last):
    """
    Summarize a network range given the first and last IP addresses.

        Example:
            >>> list(summarize_address_range(IPv4Address('192.0.2.0'),
            ...                              IPv4Address('192.0.2.130')))
            ...                                #doctest: +NORMALIZE_WHITESPACE
            [IPv4Network('192.0.2.0/25'), IPv4Network('192.0.2.128/31'),
             IPv4Network('192.0.2.130/32')]

        Args:
            first: the first IPv4Address or IPv6Address in the range.
            last: the last IPv4Address or IPv6Address in the range.

        Returns:
            An iterator of the summarized IPv(4|6) network objects.

        Raise:
            TypeError:
                If the first and last objects are not IP addresses.
                If the first and last objects are not the same version.
            ValueError:
                If the last object is not greater than the first.
                If the version of the first address is not 4 or 6.

    
    """
def _collapse_addresses_internal(addresses):
    """
    Loops through the addresses, collapsing concurrent netblocks.

        Example:

            ip1 = IPv4Network('192.0.2.0/26')
            ip2 = IPv4Network('192.0.2.64/26')
            ip3 = IPv4Network('192.0.2.128/26')
            ip4 = IPv4Network('192.0.2.192/26')

            _collapse_addresses_internal([ip1, ip2, ip3, ip4]) ->
              [IPv4Network('192.0.2.0/24')]

            This shouldn't be called directly; it is called via
              collapse_addresses([]).

        Args:
            addresses: A list of IPv4Network's or IPv6Network's

        Returns:
            A list of IPv4Network's or IPv6Network's depending on what we were
            passed.

    
    """
def collapse_addresses(addresses):
    """
    Collapse a list of IP objects.

        Example:
            collapse_addresses([IPv4Network('192.0.2.0/25'),
                                IPv4Network('192.0.2.128/25')]) ->
                               [IPv4Network('192.0.2.0/24')]

        Args:
            addresses: An iterator of IPv4Network or IPv6Network objects.

        Returns:
            An iterator of the collapsed IPv(4|6)Network objects.

        Raises:
            TypeError: If passed a list of mixed version objects.

    
    """
def get_mixed_type_key(obj):
    """
    Return a key suitable for sorting between networks and addresses.

        Address and Network objects are not sortable by default; they're
        fundamentally different so the expression

            IPv4Address('192.0.2.0') <= IPv4Network('192.0.2.0/24')

        doesn't make any sense.  There are some times however, where you may wish
        to have ipaddress sort these for you anyway. If you need to do this, you
        can use this function as the key= argument to sorted().

        Args:
          obj: either a Network or Address object.
        Returns:
          appropriate key.

    
    """
def _IPAddressBase:
    """
    The mother class.
    """
    def exploded(self):
        """
        Return the longhand version of the IP address as a string.
        """
    def compressed(self):
        """
        Return the shorthand version of the IP address as a string.
        """
    def reverse_pointer(self):
        """
        The name of the reverse DNS pointer for the IP address, e.g.:
                    >>> ipaddress.ip_address("127.0.0.1").reverse_pointer
                    '1.0.0.127.in-addr.arpa'
                    >>> ipaddress.ip_address("2001:db8::1").reverse_pointer
                    '1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.d.0.1.0.0.2.ip6.arpa'

        
        """
    def version(self):
        """
        '%200s has no version specified'
        """
    def _check_int_address(self, address):
        """
        %d (< 0) is not permitted as an IPv%d address
        """
    def _check_packed_address(self, address, expected_len):
        """
        %r (len %d != %d) is not permitted as an IPv%d address
        """
    def _ip_int_from_prefix(cls, prefixlen):
        """
        Turn the prefix length into a bitwise netmask

                Args:
                    prefixlen: An integer, the prefix length.

                Returns:
                    An integer.

        
        """
    def _prefix_from_ip_int(cls, ip_int):
        """
        Return prefix length from the bitwise netmask.

                Args:
                    ip_int: An integer, the netmask in expanded bitwise format

                Returns:
                    An integer, the prefix length.

                Raises:
                    ValueError: If the input intermingles zeroes & ones
        
        """
    def _report_invalid_netmask(cls, netmask_str):
        """
        '%r is not a valid netmask'
        """
    def _prefix_from_prefix_string(cls, prefixlen_str):
        """
        Return prefix length from a numeric string

                Args:
                    prefixlen_str: The string to be converted

                Returns:
                    An integer, the prefix length.

                Raises:
                    NetmaskValueError: If the input is not a valid netmask
        
        """
    def _prefix_from_ip_string(cls, ip_str):
        """
        Turn a netmask/hostmask string into a prefix length

                Args:
                    ip_str: The netmask/hostmask to be converted

                Returns:
                    An integer, the prefix length.

                Raises:
                    NetmaskValueError: If the input is not a valid netmask/hostmask
        
        """
    def _split_addr_prefix(cls, address):
        """
        Helper function to parse address of Network/Interface.

                Arg:
                    address: Argument of Network/Interface.

                Returns:
                    (addr, prefix) tuple.
        
        """
    def __reduce__(self):
        """
        A generic IP object.

            This IP class contains the version independent methods which are
            used by single IP addresses.
    
        """
    def __int__(self):
        """
        '%s and %s are not of the same version'
        """
    def __add__(self, other):
        """
        '%s(%r)'
        """
    def __str__(self):
        """
        A generic IP network object.

            This IP class contains the version independent methods which are
            used by networks.
    
        """
    def __repr__(self):
        """
        '%s(%r)'
        """
    def __str__(self):
        """
        '%s/%d'
        """
    def hosts(self):
        """
        Generate Iterator over usable hosts in a network.

                This is like __iter__ except it doesn't return the network
                or broadcast addresses.

        
        """
    def __iter__(self):
        """
        'address out of range'
        """
    def __lt__(self, other):
        """
        '%s and %s are not of the same version'
        """
    def __eq__(self, other):
        """
         always false if one is v4 and the other is v6.

        """
    def overlaps(self, other):
        """
        Tell if self is partly contained in other.
        """
    def broadcast_address(self):
        """
        '%s/%d'
        """
    def with_netmask(self):
        """
        '%s/%s'
        """
    def with_hostmask(self):
        """
        '%s/%s'
        """
    def num_addresses(self):
        """
        Number of hosts in the current subnet.
        """
    def _address_class(self):
        """
         Returning bare address objects (rather than interfaces) allows for
         more consistent behaviour across the network address, broadcast
         address and individual host addresses.

        """
    def prefixlen(self):
        """
        Remove an address from a larger block.

                For example:

                    addr1 = ip_network('192.0.2.0/28')
                    addr2 = ip_network('192.0.2.1/32')
                    list(addr1.address_exclude(addr2)) =
                        [IPv4Network('192.0.2.0/32'), IPv4Network('192.0.2.2/31'),
                         IPv4Network('192.0.2.4/30'), IPv4Network('192.0.2.8/29')]

                or IPv6:

                    addr1 = ip_network('2001:db8::1/32')
                    addr2 = ip_network('2001:db8::1/128')
                    list(addr1.address_exclude(addr2)) =
                        [ip_network('2001:db8::1/128'),
                         ip_network('2001:db8::2/127'),
                         ip_network('2001:db8::4/126'),
                         ip_network('2001:db8::8/125'),
                         ...
                         ip_network('2001:db8:8000::/33')]

                Args:
                    other: An IPv4Network or IPv6Network object of the same type.

                Returns:
                    An iterator of the IPv(4|6)Network objects which is self
                    minus other.

                Raises:
                    TypeError: If self and other are of differing address
                      versions, or if other is not a network object.
                    ValueError: If other is not completely contained by self.

        
        """
    def compare_networks(self, other):
        """
        Compare two IP objects.

                This is only concerned about the comparison of the integer
                representation of the network addresses.  This means that the
                host bits aren't considered at all in this method.  If you want
                to compare host bits, you can easily enough do a
                'HostA._ip < HostB._ip'

                Args:
                    other: An IP object.

                Returns:
                    If the IP versions of self and other are the same, returns:

                    -1 if self < other:
                      eg: IPv4Network('192.0.2.0/25') < IPv4Network('192.0.2.128/25')
                      IPv6Network('2001:db8::1000/124') <
                          IPv6Network('2001:db8::2000/124')
                    0 if self == other
                      eg: IPv4Network('192.0.2.0/24') == IPv4Network('192.0.2.0/24')
                      IPv6Network('2001:db8::1000/124') ==
                          IPv6Network('2001:db8::1000/124')
                    1 if self > other
                      eg: IPv4Network('192.0.2.128/25') > IPv4Network('192.0.2.0/25')
                          IPv6Network('2001:db8::2000/124') >
                              IPv6Network('2001:db8::1000/124')

                  Raises:
                      TypeError if the IP versions are different.

        
        """
    def _get_networks_key(self):
        """
        Network-only key function.

                Returns an object that identifies this address' network and
                netmask. This function is a suitable "key" argument for sorted()
                and list.sort().

        
        """
    def subnets(self, prefixlen_diff=1, new_prefix=None):
        """
        The subnets which join to make the current subnet.

                In the case that self contains only one IP
                (self._prefixlen == 32 for IPv4 or self._prefixlen == 128
                for IPv6), yield an iterator with just ourself.

                Args:
                    prefixlen_diff: An integer, the amount the prefix length
                      should be increased by. This should not be set if
                      new_prefix is also set.
                    new_prefix: The desired new prefix length. This must be a
                      larger number (smaller prefix) than the existing prefix.
                      This should not be set if prefixlen_diff is also set.

                Returns:
                    An iterator of IPv(4|6) objects.

                Raises:
                    ValueError: The prefixlen_diff is too small or too large.
                        OR
                    prefixlen_diff and new_prefix are both set or new_prefix
                      is a smaller number than the current prefix (smaller
                      number means a larger network)

        
        """
    def supernet(self, prefixlen_diff=1, new_prefix=None):
        """
        The supernet containing the current network.

                Args:
                    prefixlen_diff: An integer, the amount the prefix length of
                      the network should be decreased by.  For example, given a
                      /24 network and a prefixlen_diff of 3, a supernet with a
                      /21 netmask is returned.

                Returns:
                    An IPv4 network object.

                Raises:
                    ValueError: If self.prefixlen - prefixlen_diff < 0. I.e., you have
                      a negative prefix length.
                        OR
                    If prefixlen_diff and new_prefix are both set or new_prefix is a
                      larger number than the current prefix (larger number means a
                      smaller network)

        
        """
    def is_multicast(self):
        """
        Test if the address is reserved for multicast use.

                Returns:
                    A boolean, True if the address is a multicast address.
                    See RFC 2373 2.7 for details.

        
        """
    def _is_subnet_of(a, b):
        """
         Always false if one is v4 and the other is v6.

        """
    def subnet_of(self, other):
        """
        Return True if this network is a subnet of other.
        """
    def supernet_of(self, other):
        """
        Return True if this network is a supernet of other.
        """
    def is_reserved(self):
        """
        Test if the address is otherwise IETF reserved.

                Returns:
                    A boolean, True if the address is within one of the
                    reserved IPv6 Network ranges.

        
        """
    def is_link_local(self):
        """
        Test if the address is reserved for link-local.

                Returns:
                    A boolean, True if the address is reserved per RFC 4291.

        
        """
    def is_private(self):
        """
        Test if this address is allocated for private networks.

                Returns:
                    A boolean, True if the address is reserved per
                    iana-ipv4-special-registry or iana-ipv6-special-registry.

        
        """
    def is_global(self):
        """
        Test if this address is allocated for public networks.

                Returns:
                    A boolean, True if the address is not reserved per
                    iana-ipv4-special-registry or iana-ipv6-special-registry.

        
        """
    def is_unspecified(self):
        """
        Test if the address is unspecified.

                Returns:
                    A boolean, True if this is the unspecified address as defined in
                    RFC 2373 2.5.2.

        
        """
    def is_loopback(self):
        """
        Test if the address is a loopback address.

                Returns:
                    A boolean, True if the address is a loopback address as defined in
                    RFC 2373 2.5.3.

        
        """
def _BaseV4:
    """
    Base IPv4 object.

        The following methods are used by IPv4 objects in both single IP
        addresses and networks.

    
    """
    def _explode_shorthand_ip_string(self):
        """
        Make a (netmask, prefix_len) tuple from the given argument.

                Argument can be:
                - an integer (the prefix length)
                - a string representing the prefix length (e.g. "24")
                - a string representing the prefix netmask (e.g. "255.255.255.0")
        
        """
    def _ip_int_from_string(cls, ip_str):
        """
        Turn the given IP string into an integer for comparison.

                Args:
                    ip_str: A string, the IP ip_str.

                Returns:
                    The IP ip_str as an integer.

                Raises:
                    AddressValueError: if ip_str isn't a valid IPv4 Address.

        
        """
    def _parse_octet(cls, octet_str):
        """
        Convert a decimal octet into an integer.

                Args:
                    octet_str: A string, the number to parse.

                Returns:
                    The octet as an integer.

                Raises:
                    ValueError: if the octet isn't strictly a decimal from [0..255].

        
        """
    def _string_from_ip_int(cls, ip_int):
        """
        Turns a 32-bit integer into dotted decimal notation.

                Args:
                    ip_int: An integer, the IP address.

                Returns:
                    The IP address as a string in dotted decimal notation.

        
        """
    def _reverse_pointer(self):
        """
        Return the reverse DNS pointer name for the IPv4 address.

                This implements the method described in RFC1035 3.5.

        
        """
    def max_prefixlen(self):
        """
        Represent and manipulate single IPv4 Addresses.
        """
    def __init__(self, address):
        """

                Args:
                    address: A string or integer representing the IP

                      Additionally, an integer can be passed, so
                      IPv4Address('192.0.2.1') == IPv4Address(3221225985).
                      or, more generally
                      IPv4Address(int(IPv4Address('192.0.2.1'))) ==
                        IPv4Address('192.0.2.1')

                Raises:
                    AddressValueError: If ipaddress isn't a valid IPv4 address.

        
        """
    def packed(self):
        """
        The binary representation of this address.
        """
    def is_reserved(self):
        """
        Test if the address is otherwise IETF reserved.

                 Returns:
                     A boolean, True if the address is within the
                     reserved IPv4 Network range.

        
        """
    def is_private(self):
        """
        Test if this address is allocated for private networks.

                Returns:
                    A boolean, True if the address is reserved per
                    iana-ipv4-special-registry.

        
        """
    def is_global(self):
        """
        Test if the address is reserved for multicast use.

                Returns:
                    A boolean, True if the address is multicast.
                    See RFC 3171 for details.

        
        """
    def is_unspecified(self):
        """
        Test if the address is unspecified.

                Returns:
                    A boolean, True if this is the unspecified address as defined in
                    RFC 5735 3.

        
        """
    def is_loopback(self):
        """
        Test if the address is a loopback address.

                Returns:
                    A boolean, True if the address is a loopback per RFC 3330.

        
        """
    def is_link_local(self):
        """
        Test if the address is reserved for link-local.

                Returns:
                    A boolean, True if the address is link-local per RFC 3927.

        
        """
def IPv4Interface(IPv4Address):
    """
    '%s/%d'
    """
    def __eq__(self, other):
        """
         An interface with an associated network is NOT the
         same as an unassociated address. That's why the hash
         takes the extra info into account.

        """
    def __lt__(self, other):
        """
         We *do* allow addresses and interfaces to be sorted. The
         unassociated address is considered less than all interfaces.

        """
    def __hash__(self):
        """
        '%s/%s'
        """
    def with_netmask(self):
        """
        '%s/%s'
        """
    def with_hostmask(self):
        """
        '%s/%s'
        """
def IPv4Network(_BaseV4, _BaseNetwork):
    """
    This class represents and manipulates 32-bit IPv4 network + addresses..

        Attributes: [examples for IPv4Network('192.0.2.0/27')]
            .network_address: IPv4Address('192.0.2.0')
            .hostmask: IPv4Address('0.0.0.31')
            .broadcast_address: IPv4Address('192.0.2.32')
            .netmask: IPv4Address('255.255.255.224')
            .prefixlen: 27

    
    """
    def __init__(self, address, strict=True):
        """
        Instantiate a new IPv4 network object.

                Args:
                    address: A string or integer representing the IP [& network].
                      '192.0.2.0/24'
                      '192.0.2.0/255.255.255.0'
                      '192.0.0.2/0.0.0.255'
                      are all functionally the same in IPv4. Similarly,
                      '192.0.2.1'
                      '192.0.2.1/255.255.255.255'
                      '192.0.2.1/32'
                      are also functionally equivalent. That is to say, failing to
                      provide a subnetmask will create an object with a mask of /32.

                      If the mask (portion after the / in the argument) is given in
                      dotted quad form, it is treated as a netmask if it starts with a
                      non-zero field (e.g. /255.0.0.0 == /8) and as a hostmask if it
                      starts with a zero field (e.g. 0.255.255.255 == /8), with the
                      single exception of an all-zero mask which is treated as a
                      netmask == /0. If no mask is given, a default of /32 is used.

                      Additionally, an integer can be passed, so
                      IPv4Network('192.0.2.1') == IPv4Network(3221225985)
                      or, more generally
                      IPv4Interface(int(IPv4Interface('192.0.2.1'))) ==
                        IPv4Interface('192.0.2.1')

                Raises:
                    AddressValueError: If ipaddress isn't a valid IPv4 address.
                    NetmaskValueError: If the netmask isn't valid for
                      an IPv4 address.
                    ValueError: If strict is True and a network address is not
                      supplied.
        
        """
    def is_global(self):
        """
        Test if this address is allocated for public networks.

                Returns:
                    A boolean, True if the address is not reserved per
                    iana-ipv4-special-registry.

        
        """
def _IPv4Constants:
    """
    '169.254.0.0/16'
    """
def _BaseV6:
    """
    Base IPv6 object.

        The following methods are used by IPv6 objects in both single IP
        addresses and networks.

    
    """
    def _make_netmask(cls, arg):
        """
        Make a (netmask, prefix_len) tuple from the given argument.

                Argument can be:
                - an integer (the prefix length)
                - a string representing the prefix length (e.g. "24")
                - a string representing the prefix netmask (e.g. "255.255.255.0")
        
        """
    def _ip_int_from_string(cls, ip_str):
        """
        Turn an IPv6 ip_str into an integer.

                Args:
                    ip_str: A string, the IPv6 ip_str.

                Returns:
                    An int, the IPv6 address

                Raises:
                    AddressValueError: if ip_str isn't a valid IPv6 Address.

        
        """
    def _parse_hextet(cls, hextet_str):
        """
        Convert an IPv6 hextet string into an integer.

                Args:
                    hextet_str: A string, the number to parse.

                Returns:
                    The hextet as an integer.

                Raises:
                    ValueError: if the input isn't strictly a hex number from
                      [0..FFFF].

        
        """
    def _compress_hextets(cls, hextets):
        """
        Compresses a list of hextets.

                Compresses a list of strings, replacing the longest continuous
                sequence of "0" in the list with "" and adding empty strings at
                the beginning or at the end of the string such that subsequently
                calling ":".join(hextets) will produce the compressed version of
                the IPv6 address.

                Args:
                    hextets: A list of strings, the hextets to compress.

                Returns:
                    A list of strings.

        
        """
    def _string_from_ip_int(cls, ip_int=None):
        """
        Turns a 128-bit integer into hexadecimal notation.

                Args:
                    ip_int: An integer, the IP address.

                Returns:
                    A string, the hexadecimal representation of the address.

                Raises:
                    ValueError: The address is bigger than 128 bits of all ones.

        
        """
    def _explode_shorthand_ip_string(self):
        """
        Expand a shortened IPv6 address.

                Args:
                    ip_str: A string, the IPv6 address.

                Returns:
                    A string, the expanded IPv6 address.

        
        """
    def _reverse_pointer(self):
        """
        Return the reverse DNS pointer name for the IPv6 address.

                This implements the method described in RFC3596 2.5.

        
        """
    def max_prefixlen(self):
        """
        Represent and manipulate single IPv6 Addresses.
        """
    def __init__(self, address):
        """
        Instantiate a new IPv6 address object.

                Args:
                    address: A string or integer representing the IP

                      Additionally, an integer can be passed, so
                      IPv6Address('2001:db8::') ==
                        IPv6Address(42540766411282592856903984951653826560)
                      or, more generally
                      IPv6Address(int(IPv6Address('2001:db8::'))) ==
                        IPv6Address('2001:db8::')

                Raises:
                    AddressValueError: If address isn't a valid IPv6 address.

        
        """
    def packed(self):
        """
        The binary representation of this address.
        """
    def is_multicast(self):
        """
        Test if the address is reserved for multicast use.

                Returns:
                    A boolean, True if the address is a multicast address.
                    See RFC 2373 2.7 for details.

        
        """
    def is_reserved(self):
        """
        Test if the address is otherwise IETF reserved.

                Returns:
                    A boolean, True if the address is within one of the
                    reserved IPv6 Network ranges.

        
        """
    def is_link_local(self):
        """
        Test if the address is reserved for link-local.

                Returns:
                    A boolean, True if the address is reserved per RFC 4291.

        
        """
    def is_site_local(self):
        """
        Test if the address is reserved for site-local.

                Note that the site-local address space has been deprecated by RFC 3879.
                Use is_private to test if this address is in the space of unique local
                addresses as defined by RFC 4193.

                Returns:
                    A boolean, True if the address is reserved per RFC 3513 2.5.6.

        
        """
    def is_private(self):
        """
        Test if this address is allocated for private networks.

                Returns:
                    A boolean, True if the address is reserved per
                    iana-ipv6-special-registry.

        
        """
    def is_global(self):
        """
        Test if this address is allocated for public networks.

                Returns:
                    A boolean, true if the address is not reserved per
                    iana-ipv6-special-registry.

        
        """
    def is_unspecified(self):
        """
        Test if the address is unspecified.

                Returns:
                    A boolean, True if this is the unspecified address as defined in
                    RFC 2373 2.5.2.

        
        """
    def is_loopback(self):
        """
        Test if the address is a loopback address.

                Returns:
                    A boolean, True if the address is a loopback address as defined in
                    RFC 2373 2.5.3.

        
        """
    def ipv4_mapped(self):
        """
        Return the IPv4 mapped address.

                Returns:
                    If the IPv6 address is a v4 mapped address, return the
                    IPv4 mapped address. Return None otherwise.

        
        """
    def teredo(self):
        """
        Tuple of embedded teredo IPs.

                Returns:
                    Tuple of the (server, client) IPs or None if the address
                    doesn't appear to be a teredo address (doesn't start with
                    2001::/32)

        
        """
    def sixtofour(self):
        """
        Return the IPv4 6to4 embedded address.

                Returns:
                    The IPv4 6to4-embedded address if present or None if the
                    address doesn't appear to contain a 6to4 embedded address.

        
        """
def IPv6Interface(IPv6Address):
    """
    '%s/%d'
    """
    def __eq__(self, other):
        """
         An interface with an associated network is NOT the
         same as an unassociated address. That's why the hash
         takes the extra info into account.

        """
    def __lt__(self, other):
        """
         We *do* allow addresses and interfaces to be sorted. The
         unassociated address is considered less than all interfaces.

        """
    def __hash__(self):
        """
        '%s/%s'
        """
    def with_netmask(self):
        """
        '%s/%s'
        """
    def with_hostmask(self):
        """
        '%s/%s'
        """
    def is_unspecified(self):
        """
        This class represents and manipulates 128-bit IPv6 networks.

            Attributes: [examples for IPv6('2001:db8::1000/124')]
                .network_address: IPv6Address('2001:db8::1000')
                .hostmask: IPv6Address('::f')
                .broadcast_address: IPv6Address('2001:db8::100f')
                .netmask: IPv6Address('ffff:ffff:ffff:ffff:ffff:ffff:ffff:fff0')
                .prefixlen: 124

    
        """
    def __init__(self, address, strict=True):
        """
        Instantiate a new IPv6 Network object.

                Args:
                    address: A string or integer representing the IPv6 network or the
                      IP and prefix/netmask.
                      '2001:db8::/128'
                      '2001:db8:0000:0000:0000:0000:0000:0000/128'
                      '2001:db8::'
                      are all functionally the same in IPv6.  That is to say,
                      failing to provide a subnetmask will create an object with
                      a mask of /128.

                      Additionally, an integer can be passed, so
                      IPv6Network('2001:db8::') ==
                        IPv6Network(42540766411282592856903984951653826560)
                      or, more generally
                      IPv6Network(int(IPv6Network('2001:db8::'))) ==
                        IPv6Network('2001:db8::')

                    strict: A boolean. If true, ensure that we have been passed
                      A true network address, eg, 2001:db8::1000/124 and not an
                      IP address on a network, eg, 2001:db8::1/124.

                Raises:
                    AddressValueError: If address isn't a valid IPv6 address.
                    NetmaskValueError: If the netmask isn't valid for
                      an IPv6 address.
                    ValueError: If strict was True and a network address was not
                      supplied.
        
        """
    def hosts(self):
        """
        Generate Iterator over usable hosts in a network.

                  This is like __iter__ except it doesn't return the
                  Subnet-Router anycast address.

        
        """
    def is_site_local(self):
        """
        Test if the address is reserved for site-local.

                Note that the site-local address space has been deprecated by RFC 3879.
                Use is_private to test if this address is in the space of unique local
                addresses as defined by RFC 4193.

                Returns:
                    A boolean, True if the address is reserved per RFC 3513 2.5.6.

        
        """
def _IPv6Constants:
    """
    'fe80::/10'
    """
