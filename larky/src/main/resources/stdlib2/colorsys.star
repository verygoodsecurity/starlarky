def rgb_to_yiq(r, g, b):
    """
     r = y + (0.27*q + 0.41*i) / (0.74*0.41 + 0.27*0.48)
     b = y + (0.74*q - 0.48*i) / (0.74*0.41 + 0.27*0.48)
     g = y - (0.30*(r-y) + 0.11*(b-y)) / 0.59


    """
def rgb_to_hls(r, g, b):
    """
     XXX Can optimize (maxc+minc) and (maxc-minc)

    """
def hls_to_rgb(h, l, s):
    """
     HSV: Hue, Saturation, Value
     H: position in the spectrum
     S: color saturation ("purity")
     V: color brightness


    """
def rgb_to_hsv(r, g, b):
    """
     XXX assume int() truncates!
    """
