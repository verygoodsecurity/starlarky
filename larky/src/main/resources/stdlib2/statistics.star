def StatisticsError(ValueError):
    """
     === Private utilities ===


    """
def _sum(data, start=0):
    """
    _sum(data [, start]) -> (type, sum, count)

        Return a high-precision sum of the given numeric data as a fraction,
        together with the type to be converted to and the count of items.

        If optional argument ``start`` is given, it is added to the total.
        If ``data`` is empty, ``start`` (defaulting to 0) is returned.


        Examples
        --------

        >>> _sum([3, 2.25, 4.5, -0.5, 1.0], 0.75)
        (<class 'float'>, Fraction(11, 1), 5)

        Some sources of round-off error will be avoided:

        # Built-in sum returns zero.
        >>> _sum([1e50, 1, -1e50] * 1000)
        (<class 'float'>, Fraction(1000, 1), 3000)

        Fractions and Decimals are also supported:

        >>> from fractions import Fraction as F
        >>> _sum([F(2, 3), F(7, 5), F(1, 4), F(5, 6)])
        (<class 'fractions.Fraction'>, Fraction(63, 20), 4)

        >>> from decimal import Decimal as D
        >>> data = [D("0.1375"), D("0.2108"), D("0.3061"), D("0.0419")]
        >>> _sum(data)
        (<class 'decimal.Decimal'>, Fraction(6963, 10000), 4)

        Mixed types are currently treated as an error, except that int is
        allowed.
    
    """
def _isfinite(x):
    """
     Likely a Decimal.
    """
def _coerce(T, S):
    """
    Coerce types T and S to a common type, or raise TypeError.

        Coercion rules are currently an implementation detail. See the CoerceTest
        test class in test_statistics for details.
    
    """
def _exact_ratio(x):
    """
    Return Real number x to exact (numerator, denominator) pair.

        >>> _exact_ratio(0.25)
        (1, 4)

        x is expected to be an int, Fraction, Decimal or float.
    
    """
def _convert(value, T):
    """
    Convert value to given numeric type T.
    """
def _find_lteq(a, x):
    """
    'Locate the leftmost value exactly equal to x'
    """
def _find_rteq(a, l, x):
    """
    'Locate the rightmost value exactly equal to x'
    """
def _fail_neg(values, errmsg='negative value'):
    """
    Iterate over values, failing if any are less than zero.
    """
def mean(data):
    """
    Return the sample arithmetic mean of data.

        >>> mean([1, 2, 3, 4, 4])
        2.8

        >>> from fractions import Fraction as F
        >>> mean([F(3, 7), F(1, 21), F(5, 3), F(1, 3)])
        Fraction(13, 21)

        >>> from decimal import Decimal as D
        >>> mean([D("0.5"), D("0.75"), D("0.625"), D("0.375")])
        Decimal('0.5625')

        If ``data`` is empty, StatisticsError will be raised.
    
    """
def fmean(data):
    """
    Convert data to floats and compute the arithmetic mean.

        This runs faster than the mean() function and it always returns a float.
        If the input dataset is empty, it raises a StatisticsError.

        >>> fmean([3.5, 4.0, 5.25])
        4.25
    
    """
        def count(iterable):
            """
            'fmean requires at least one data point'
            """
def geometric_mean(data):
    """
    Convert data to floats and compute the geometric mean.

        Raises a StatisticsError if the input dataset is empty,
        if it contains a zero, or if it contains a negative value.

        No special efforts are made to achieve exact results.
        (However, this may change in the future.)

        >>> round(geometric_mean([54, 24, 36]), 9)
        36.0
    
    """
def harmonic_mean(data):
    """
    Return the harmonic mean of data.

        The harmonic mean, sometimes called the subcontrary mean, is the
        reciprocal of the arithmetic mean of the reciprocals of the data,
        and is often appropriate when averaging quantities which are rates
        or ratios, for example speeds. Example:

        Suppose an investor purchases an equal value of shares in each of
        three companies, with P/E (price/earning) ratios of 2.5, 3 and 10.
        What is the average P/E ratio for the investor's portfolio?

        >>> harmonic_mean([2.5, 3, 10])  # For an equal investment portfolio.
        3.6

        Using the arithmetic mean would give an average of about 5.167, which
        is too high.

        If ``data`` is empty, or any element is less than zero,
        ``harmonic_mean`` will raise ``StatisticsError``.
    
    """
def median(data):
    """
    Return the median (middle value) of numeric data.

        When the number of data points is odd, return the middle data point.
        When the number of data points is even, the median is interpolated by
        taking the average of the two middle values:

        >>> median([1, 3, 5])
        3
        >>> median([1, 3, 5, 7])
        4.0

    
    """
def median_low(data):
    """
    Return the low median of numeric data.

        When the number of data points is odd, the middle value is returned.
        When it is even, the smaller of the two middle values is returned.

        >>> median_low([1, 3, 5])
        3
        >>> median_low([1, 3, 5, 7])
        3

    
    """
def median_high(data):
    """
    Return the high median of data.

        When the number of data points is odd, the middle value is returned.
        When it is even, the larger of the two middle values is returned.

        >>> median_high([1, 3, 5])
        3
        >>> median_high([1, 3, 5, 7])
        5

    
    """
def median_grouped(data, interval=1):
    """
    Return the 50th percentile (median) of grouped continuous data.

        >>> median_grouped([1, 2, 2, 3, 4, 4, 4, 4, 4, 5])
        3.7
        >>> median_grouped([52, 52, 53, 54])
        52.5

        This calculates the median as the 50th percentile, and should be
        used when your data is continuous and grouped. In the above example,
        the values 1, 2, 3, etc. actually represent the midpoint of classes
        0.5-1.5, 1.5-2.5, 2.5-3.5, etc. The middle value falls somewhere in
        class 3.5-4.5, and interpolation is used to estimate it.

        Optional argument ``interval`` represents the class interval, and
        defaults to 1. Changing the class interval naturally will change the
        interpolated 50th percentile value:

        >>> median_grouped([1, 3, 3, 5, 7], interval=1)
        3.25
        >>> median_grouped([1, 3, 3, 5, 7], interval=2)
        3.5

        This function does not check whether the data points are at least
        ``interval`` apart.
    
    """
def mode(data):
    """
    Return the most common data point from discrete or nominal data.

        ``mode`` assumes discrete data, and returns a single value. This is the
        standard treatment of the mode as commonly taught in schools:

            >>> mode([1, 1, 2, 3, 3, 3, 3, 4])
            3

        This also works with nominal (non-numeric) data:

            >>> mode(["red", "blue", "blue", "red", "green", "red", "red"])
            'red'

        If there are multiple modes with same frequency, return the first one
        encountered:

            >>> mode(['red', 'red', 'green', 'blue', 'blue'])
            'red'

        If *data* is empty, ``mode``, raises StatisticsError.

    
    """
def multimode(data):
    """
    Return a list of the most frequently occurring values.

        Will return more than one result if there are multiple modes
        or an empty list if *data* is empty.

        >>> multimode('aabbbbbbbbcc')
        ['b']
        >>> multimode('aabbbbccddddeeffffgg')
        ['b', 'd', 'f']
        >>> multimode('')
        []
    
    """
def quantiles(data, *, n=4, method='exclusive'):
    """
    Divide *data* into *n* continuous intervals with equal probability.

        Returns a list of (n - 1) cut points separating the intervals.

        Set *n* to 4 for quartiles (the default).  Set *n* to 10 for deciles.
        Set *n* to 100 for percentiles which gives the 99 cuts points that
        separate *data* in to 100 equal sized groups.

        The *data* can be any iterable containing sample.
        The cut points are linearly interpolated between data points.

        If *method* is set to *inclusive*, *data* is treated as population
        data.  The minimum value is treated as the 0th percentile and the
        maximum value is treated as the 100th percentile.
    
    """
def _ss(data, c=None):
    """
    Return sum of square deviations of sequence data.

        If ``c`` is None, the mean is calculated in one pass, and the deviations
        from the mean are calculated in a second pass. Otherwise, deviations are
        calculated from ``c`` as given. Use the second case with care, as it can
        lead to garbage results.
    
    """
def variance(data, xbar=None):
    """
    Return the sample variance of data.

        data should be an iterable of Real-valued numbers, with at least two
        values. The optional argument xbar, if given, should be the mean of
        the data. If it is missing or None, the mean is automatically calculated.

        Use this function when your data is a sample from a population. To
        calculate the variance from the entire population, see ``pvariance``.

        Examples:

        >>> data = [2.75, 1.75, 1.25, 0.25, 0.5, 1.25, 3.5]
        >>> variance(data)
        1.3720238095238095

        If you have already calculated the mean of your data, you can pass it as
        the optional second argument ``xbar`` to avoid recalculating it:

        >>> m = mean(data)
        >>> variance(data, m)
        1.3720238095238095

        This function does not check that ``xbar`` is actually the mean of
        ``data``. Giving arbitrary values for ``xbar`` may lead to invalid or
        impossible results.

        Decimals and Fractions are supported:

        >>> from decimal import Decimal as D
        >>> variance([D("27.5"), D("30.25"), D("30.25"), D("34.5"), D("41.75")])
        Decimal('31.01875')

        >>> from fractions import Fraction as F
        >>> variance([F(1, 6), F(1, 2), F(5, 3)])
        Fraction(67, 108)

    
    """
def pvariance(data, mu=None):
    """
    Return the population variance of ``data``.

        data should be a sequence or iterable of Real-valued numbers, with at least one
        value. The optional argument mu, if given, should be the mean of
        the data. If it is missing or None, the mean is automatically calculated.

        Use this function to calculate the variance from the entire population.
        To estimate the variance from a sample, the ``variance`` function is
        usually a better choice.

        Examples:

        >>> data = [0.0, 0.25, 0.25, 1.25, 1.5, 1.75, 2.75, 3.25]
        >>> pvariance(data)
        1.25

        If you have already calculated the mean of the data, you can pass it as
        the optional second argument to avoid recalculating it:

        >>> mu = mean(data)
        >>> pvariance(data, mu)
        1.25

        Decimals and Fractions are supported:

        >>> from decimal import Decimal as D
        >>> pvariance([D("27.5"), D("30.25"), D("30.25"), D("34.5"), D("41.75")])
        Decimal('24.815')

        >>> from fractions import Fraction as F
        >>> pvariance([F(1, 4), F(5, 4), F(1, 2)])
        Fraction(13, 72)

    
    """
def stdev(data, xbar=None):
    """
    Return the square root of the sample variance.

        See ``variance`` for arguments and other details.

        >>> stdev([1.5, 2.5, 2.5, 2.75, 3.25, 4.75])
        1.0810874155219827

    
    """
def pstdev(data, mu=None):
    """
    Return the square root of the population variance.

        See ``pvariance`` for arguments and other details.

        >>> pstdev([1.5, 2.5, 2.5, 2.75, 3.25, 4.75])
        0.986893273527251

    
    """
def _normal_dist_inv_cdf(p, mu, sigma):
    """
     There is no closed-form solution to the inverse CDF for the normal
     distribution, so we use a rational approximation instead:
     Wichura, M.J. (1988). "Algorithm AS241: The Percentage Points of the
     Normal Distribution".  Applied Statistics. Blackwell Publishing. 37
     (3): 477–484. doi:10.2307/2347330. JSTOR 2347330.

    """
def NormalDist:
    """
    Normal distribution of a random variable
    """
    def __init__(self, mu=0.0, sigma=1.0):
        """
        NormalDist where mu is the mean and sigma is the standard deviation.
        """
    def from_samples(cls, data):
        """
        Make a normal distribution instance from sample data.
        """
    def samples(self, n, *, seed=None):
        """
        Generate *n* samples for a given mean and standard deviation.
        """
    def pdf(self, x):
        """
        Probability density function.  P(x <= X < x+dx) / dx
        """
    def cdf(self, x):
        """
        Cumulative distribution function.  P(X <= x)
        """
    def inv_cdf(self, p):
        """
        Inverse cumulative distribution function.  x : P(X <= x) = p

                Finds the value of the random variable such that the probability of
                the variable being less than or equal to that value equals the given
                probability.

                This function is also called the percent point function or quantile
                function.
        
        """
    def quantiles(self, n=4):
        """
        Divide into *n* continuous intervals with equal probability.

                Returns a list of (n - 1) cut points separating the intervals.

                Set *n* to 4 for quartiles (the default).  Set *n* to 10 for deciles.
                Set *n* to 100 for percentiles which gives the 99 cuts points that
                separate the normal distribution in to 100 equal sized groups.
        
        """
    def overlap(self, other):
        """
        Compute the overlapping coefficient (OVL) between two normal distributions.

                Measures the agreement between two normal probability distributions.
                Returns a value between 0.0 and 1.0 giving the overlapping area in
                the two underlying probability density functions.

                    >>> N1 = NormalDist(2.4, 1.6)
                    >>> N2 = NormalDist(3.2, 2.0)
                    >>> N1.overlap(N2)
                    0.8035050657330205
        
        """
    def mean(self):
        """
        Arithmetic mean of the normal distribution.
        """
    def median(self):
        """
        Return the median of the normal distribution
        """
    def mode(self):
        """
        Return the mode of the normal distribution

                The mode is the value x where which the probability density
                function (pdf) takes its maximum value.
        
        """
    def stdev(self):
        """
        Standard deviation of the normal distribution.
        """
    def variance(self):
        """
        Square of the standard deviation.
        """
    def __add__(x1, x2):
        """
        Add a constant or another NormalDist instance.

                If *other* is a constant, translate mu by the constant,
                leaving sigma unchanged.

                If *other* is a NormalDist, add both the means and the variances.
                Mathematically, this works only if the two distributions are
                independent or if they are jointly normally distributed.
        
        """
    def __sub__(x1, x2):
        """
        Subtract a constant or another NormalDist instance.

                If *other* is a constant, translate by the constant mu,
                leaving sigma unchanged.

                If *other* is a NormalDist, subtract the means and add the variances.
                Mathematically, this works only if the two distributions are
                independent or if they are jointly normally distributed.
        
        """
    def __mul__(x1, x2):
        """
        Multiply both mu and sigma by a constant.

                Used for rescaling, perhaps to change measurement units.
                Sigma is scaled with the absolute value of the constant.
        
        """
    def __truediv__(x1, x2):
        """
        Divide both mu and sigma by a constant.

                Used for rescaling, perhaps to change measurement units.
                Sigma is scaled with the absolute value of the constant.
        
        """
    def __pos__(x1):
        """
        Return a copy of the instance.
        """
    def __neg__(x1):
        """
        Negates mu while keeping sigma the same.
        """
    def __rsub__(x1, x2):
        """
        Subtract a NormalDist from a constant or another NormalDist.
        """
    def __eq__(x1, x2):
        """
        Two NormalDist objects are equal if their mu and sigma are both equal.
        """
    def __hash__(self):
        """
        NormalDist objects hash equal if their mu and sigma are both equal.
        """
    def __repr__(self):
        """
        f'{type(self).__name__}(mu={self._mu!r}, sigma={self._sigma!r})'
        """
    def assert_close(G1, G2):
