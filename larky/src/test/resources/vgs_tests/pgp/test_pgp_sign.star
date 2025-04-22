# load("asserts", "assert_that", "eq", "truth")
# load("vgstest_utils", setup="vgs_setup", run_test="run_test")

# def test_pgp_sign_verify():
#     """Test basic PGP signing and verification"""
#     pgp = setup().pgp
    
#     # Test key pair - do not use in production
#     private_key = """-----BEGIN PGP PRIVATE KEY BLOCK-----

# lQGVBGf0CAABDAC84Z25L9DpBInabUWNw1EOQKKlHq4/WmvyNvSQyzA6A4b6uJ/p
# EvwN1hN3TWnu9Rj0ktw1l3Qx52ggfOwH2P85a18lyiDOncyiRhxII3Nxds9XrckQ
# EIoCn9zMGC5GWD3d6hRhhr67JJAUyOBAgSZFoQXyA3BVDBNE87cbZdPRRnkUDp3S
# q7zqoMJx97R3yMcxChXrfMZi640V8/eyVHu9W8gOKZEimzSw3tx/mvs9YfusM7TN
# aMWODQyU+FuzFbaHjOhUbKZ6PEofSJenrp9Q9hWKw99iSMlsllGWX7t5yUiB1LOK
# H1n3yW+GkrxnecpdEEY0qsf9qMab/2ArhvHd+yh2D926GjJ1LOlalHOUIbZtH9Mu
# i1GY8eEcTHVafqVxdHDmRPuVJ8O/wjhUke6GoLcUo3f+pGsWCCob43jsvK18FJmB
# 4q1CawqfdzHlCQEbblYxTVUq9V5/Fz1caPuDNEhU8nLjiyMEdKA71oAD0xqc+7f9
# 112lSaFLXvJo8GkAEQEAAf8AZQBHTlUBtAlPbGVrc2FuZHKJAc0EEwEIADgWIQQT
# h9rSHu3Xe+l7cqtKC/75tgCVUgUCZ/QIAAIbAwULCQgHAgYVCgkICwIEFgIDAQIe
# AQIXgAAKCRBKC/75tgCVUiKDC/i29kiO76eqKkPfj+tgwaXrPPpSP0WcLgwEGe/6
# 0mdmQFFWb6kWgYfNfI8vbJJPzsvjfy4vY4TiBNx6ow6lQ3XMQK0OzlHsTUqF7jI2
# yw9WmY0IW4QdreJVhGZnb66gB9+fy8jZcJp52I5Pr5rtrlpO39GpiLGbemueZiMn
# mvcd015jQbXhBe/q8/K0ukSEdD0sm4NT8dJr2wIa740zb+ei1/0ovrF4/oRL2kAH
# majJSEpUabSGfFo6hNUVlL7S2LTJUX8m/cb8F+IjtF29PyZ8dWsYdtN0snlrGKtP
# WO/0DmMkwfkl0zdZybPq+W33Xbg/kcE4E3iK6dl+eODNVRkJptfAlUTuE2b0epyn
# 1ZvixfOJ23kNAFwXw0l9BzlStPDFqXp4Funkq90RWalioY7sYjyPcGfnuH4nmdmW
# MIczKAcqRiWNhujkGUKxOAg01CyNHQUuo+JlAOP9qC7q65sJBWFUT3StIR7FOhRd
# WkFuhkPtlgBcCuPWCAI95LtWmp0DmARn9AkaAQgA3e+l1etUzNX7zYs5NcIYql/r
# lhqg8bgfJg0/qE3VY5Euwq56POfcFpL2Pr6+EUbWVp7MgRSRO0mN8wFXqMxoe3nm
# y9hQTMGPYG7vylEH0RT2AoYGgfrL3s2+d5Yo5MvPRwhbdJUylDfgRLCGjRdx9ktH
# 6/FwF0ZBmjwLx4wgUuDfXDNlsKmexAWYf1lA8tE1GlOndiIUmAqGRPw/edSQdluu
# Dw0tFne2ugVu/Qqxg65Ak3GWjOcgeIMw2xBJi0qwPVe4ehVOPuk2s59RVzirk7bc
# PdOpypT6pk2AredXQVoEqitgvrgKhS9e5n+wV7ta4QTMohNPif89jNPz5zVwqwAR
# AQABAAf/XM9fk+5mV4Sy2Pl8UrlZFEKpo+t926fKSuDmeo0WDB3GJr7NOpQhi0Uo
# +gNNO69aaf3deqttspPnazRoApx8owpO1Hcg2YBmY4bw+zPYp3xzBooixY9qW0HC
# AYOHlWqPw15A1Keh767QOBl0IyOnUj6YFJiybeFBBO1QZcH5MH63PMGXIoj0IOKb
# Iyt/qOwut3gfHLcc2C6J3Yr6xsgLmjZIb0lF3MeuukjV+sB0SLjUscTqHmooiHXb
# CG0fcG9NZk4U0yw6jyyyqmfJ6Sa5GVLKZ+cYnj/G4e5dbaCqTDYdfsY9OYTDbyNO
# e0SRfKA8yeFP7jLKMyeTYffP2ySi5QQA7TQ0pVl7w1WslZaFPn2iLuURld3SIQNz
# WlrKamCJUXTydZ4VuqieRnMSuLimdjusdWkXSKIj0cTUh7Xi62cawmmVAiCVs+YY
# BqQlOVVwVW4rBDYV7BrCpkcC4M1qKb+VXCHGAgq1ljGJjCKh6jXv+GDKt4+sUzAK
# yvmG6HiOs4cEAO+Fujy6D390BSPBBoktSLI/E6rUFkQ6AQkekMnb/Kc+CMGS2m7T
# 5J/KKI4KOzmTLlwOGuGLzR7XylVDAqwGWt7ViRjDLimc9f3/SPU8eEmqtxI40DUn
# LhHLGx95Fbu0LUm1zxqLlR7FKQFM7vfUBArgwRjC7HGv3pNbUJO/k2q9A/9AdXEB
# Gs6hU7TkNOW5Q9gy3tP/k7c6Eh+2LsxskV0T9zTjl164D4O5Rexvr2MPkEOHnPbM
# 5X/wnnV7c2oQcVYuoHydzntTxTNvNA/sCEY1/lyJXYPEO8c9NScT+ca7YEW9zATo
# dZc6/Jf2kA332gs9U/D8EVyFiaJSJzDlOLTz2jz/iQLsBBgBCAAgFiEEE4fa0h7t
# 13vpe3KrSgv++bYAlVIFAmf0CRoCGwIBQAkQSgv++bYAlVLAdCAEGQEIAB0WIQT1
# PL//jUCk3zyjYXtMPKBtGfUipQUCZ/QJGgAKCRBMPKBtGfUipZ5pB/9O6Pl4u58j
# HuLoWy5AzB5Z/feIioZ+Okdgb4cdBQC6gaUzcYoEwhxMatHh1ok+tqxU8N87udTP
# SCk0C3NkJykC/O2P+lIG/weBXodZWL8gbNlriJp8/7LoxUkGz5RKErLfogTBiaT5
# Y2rN09JfzqB1xV4RcqMuOEJxNTgtlxJWEgi87tKDhmV3dCqda4PBn1fywh0Q1ugn
# 2n4zD78J+HUeKattsIV6xIBb+UTwBcfKe4En85sKsdKbZaZm3M4VIRYVfrrUVJ/a
# jEMRd3UL5RZTn3bVua4wGi1U7SuVi+R1hTF8e5NuD/5RgHZ4h1fdMN+j+toi9ked
# eTOP/M5z3ciC7jkMAIHHcUWzHk7IMYV75kGK9TFouXt9I8VIbz3FEV86DzwCYeN4
# g+TTtxcARRHvXA9wXpKCdbm28UTgCySPtaAOH2NoUSv/+QPfLsfhev+uU7j/lLgU
# 9QI6ZWqThxCKCGJ7Ca+KeBmbPzCft5L6/DzZpN6LRVLAoxucteI3PjqlYzv/ZEqy
# PSlA7BMSPzXIJUmJOY38BGRJMGymbvzzlptsSZP95ZN8aUm6EodBDbH7TGpn3F2V
# KMI+PpU/itjudSBRNbAB//8nXn8WHR3FLstDhaeRdDD2LRwHkXP/HllGaCbhEHrQ
# F6N6Aa4hypG03V6gHWz/3i0sLyLlcmQU1DctmHnoaRpunPYWWrdrvPtizBl7g8x9
# TVklRX3S36kFFbr5MWVg++tUt/yFiOvZaDP1dbOpH2a7AiSEcBCiFyBX8GQLEC45
# TsMPPqGldwm4ljmqDdm8RG+85eaNhyagVD+1UyCtjO60X41avKefcttqgFekmjXF
# XUXY17BLAM7eHQG8SQ==
# =BQF9
# -----END PGP PRIVATE KEY BLOCK-----"""

#     public_key = """-----BEGIN PGP PUBLIC KEY BLOCK-----

# mQGNBGf0CAABDAC84Z25L9DpBInabUWNw1EOQKKlHq4/WmvyNvSQyzA6A4b6uJ/p
# EvwN1hN3TWnu9Rj0ktw1l3Qx52ggfOwH2P85a18lyiDOncyiRhxII3Nxds9XrckQ
# EIoCn9zMGC5GWD3d6hRhhr67JJAUyOBAgSZFoQXyA3BVDBNE87cbZdPRRnkUDp3S
# q7zqoMJx97R3yMcxChXrfMZi640V8/eyVHu9W8gOKZEimzSw3tx/mvs9YfusM7TN
# aMWODQyU+FuzFbaHjOhUbKZ6PEofSJenrp9Q9hWKw99iSMlsllGWX7t5yUiB1LOK
# H1n3yW+GkrxnecpdEEY0qsf9qMab/2ArhvHd+yh2D926GjJ1LOlalHOUIbZtH9Mu
# i1GY8eEcTHVafqVxdHDmRPuVJ8O/wjhUke6GoLcUo3f+pGsWCCob43jsvK18FJmB
# 4q1CawqfdzHlCQEbblYxTVUq9V5/Fz1caPuDNEhU8nLjiyMEdKA71oAD0xqc+7f9
# 112lSaFLXvJo8GkAEQEAAbQJT2xla3NhbmRyiQHNBBMBCAA4FiEEE4fa0h7t13vp
# e3KrSgv++bYAlVIFAmf0CAACGwMFCwkIBwIGFQoJCAsCBBYCAwECHgECF4AACgkQ
# Sgv++bYAlVIigwv4tvZIju+nqipD34/rYMGl6zz6Uj9FnC4MBBnv+tJnZkBRVm+p
# FoGHzXyPL2yST87L438uL2OE4gTceqMOpUN1zECtDs5R7E1Khe4yNssPVpmNCFuE
# Ha3iVYRmZ2+uoAffn8vI2XCaediOT6+a7a5aTt/RqYixm3prnmYjJ5r3HdNeY0G1
# 4QXv6vPytLpEhHQ9LJuDU/HSa9sCGu+NM2/notf9KL6xeP6ES9pAB5moyUhKVGm0
# hnxaOoTVFZS+0ti0yVF/Jv3G/BfiI7RdvT8mfHVrGHbTdLJ5axirT1jv9A5jJMH5
# JdM3Wcmz6vlt9124P5HBOBN4iunZfnjgzVUZCabXwJVE7hNm9Hqcp9Wb4sXzidt5
# DQBcF8NJfQc5UrTwxal6eBbp5KvdEVmpYqGO7GI8j3Bn57h+J5nZljCHMygHKkYl
# jYbo5BlCsTgINNQsjR0FLqPiZQDj/agu6uubCQVhVE90rSEexToUXVpBboZD7ZYA
# XArj1ggCPeS7Vpq5AY0EZ/QIAAEMALtLmThKtOcowooQcUmnMiYG8cRmo97EZVnS
# QuhbuWDmdzHuP1MtFxj7CyFxbKryNA0H9OAa79NOVAugejKecq2XMFP0EgHA5ynY
# rpDfk+dKrw0YM6fUoNx0FGu36RliAex3Rrmf4H5ZcVq33gfTO8rwOo4r/MZlvtTl
# 6MXUGfW6mb2mRUUCvVfwlgKpf0Cwwu8rCf3MoC9xNfGbGmfd0D2eVja9YVqS8kwD
# +W4y4OTedpC6jkzoZLeBdgxO9OsJI5hRztWJkntkAWiuppm+IDbiwFcCF7giwl8e
# FejzlcXv8sq+TnVI+jWYyKfRtCuzdR8xab2701NRJAYMa2JjtvMiI+xiQ/fGNlAR
# XyiZ4L6ayDmpAflpAV39ToXPlQX98BIMSfyZrGWYGHtmlL4My9pMRfXYg24bWx4E
# rukizubV/FoiIC2hiwEz+FiS8LliRJq0lp/+I/EoiD3NkJn6W4fZOh5afF9DkmjV
# /GSag46txl2ChEos7mBSOcFZC6ALEwARAQABiQG2BBgBCAAgFiEEE4fa0h7t13vp
# e3KrSgv++bYAlVIFAmf0CAACGwwACgkQSgv++bYAlVKIKAwAiH4b2G3yvto4Jyxx
# qIIsak8DivHCY37xP6qVkyK6vkBfAgjKonc3UM9Yi3MvwGdLjEvrKZ/awLhUyg0j
# YmIHg3yz/2oIoxa2Pq+/LRt80YeqNhccfEJM0ZDDhUcxHOIFkhots8OesWH/wLUX
# DxJcBgt4VbYLHruSM5bw+N2OEtO3isSgMkA+kgC5SyZWyuTtSkJ7DJqjTyA4U3jF
# 4s5PsEUIbQ/6ZfRWzWEypSYmnUytOKSg/t/8p5hZO71P3EIVmdYwh9GMoZqAZgpb
# 1nN14crljTIi4N1HWQs3Amwvs24TiexPxPEhuWEy19iKnTY4XG7kH/sSBoC9yJoa
# lc8ybz5jtdfj7ieiant6Ycak0XoI+Pm1YaMC8JBABxc7yL7n2bGytT/K7YnDBIAM
# /Eo6Mf+r97ikto7K6gHaLNfAI6ZWneWhgKAPQnou20d/bGgiPjpaRD8N01Bpbncq
# x/UfxLWCjomRw0rjpjkoySoVsVdhQtCUXUv3kOHFydyOjSzq
# =LMuI
# -----END PGP PUBLIC KEY BLOCK-----"""

#     # Test message and file name
#     message = bytes("This is a test message for PGP signing", "utf-8")
#     file_name = "test.txt"
    
#     # Test sign with SHA-256
#     signed_message = pgp.sign(
#         message=message,
#         private_key=private_key,
#         file_name=file_name,
#         hash_algorithm="SHA-256",
#         armor=True
#     )
    
#     # Verify it's signed (should start with -----BEGIN PGP MESSAGE-----)
#     signed_text = str(signed_message, "utf-8")
#     assert_that(signed_text).contains("-----BEGIN PGP MESSAGE-----")
    
#     # Verify the signature
#     verified = pgp.verify(
#         signed_message=signed_message,
#         public_key=public_key
#     )
    
#     # Verify the verification worked
#     assert_that(verified).is_equal_to(True)
    
#     # Test with different hash algorithms
#     hash_algorithms = ["SHA-1", "SHA-256", "SHA-384", "SHA-512"]
    
#     for hash_algo in hash_algorithms:
#         print(f"Testing with hash algorithm: {hash_algo}")
        
#         # Sign with this algorithm
#         signed = pgp.sign(
#             message=message,
#             private_key=private_key,
#             file_name=file_name,
#             hash_algorithm=hash_algo,
#             armor=True
#         )
        
#         # Verify the signature
#         is_valid = pgp.verify(
#             signed_message=signed,
#             public_key=public_key
#         )
        
#         # Check that validation worked
#         assert_that(is_valid).is_equal_to(True)
    
#     print("All signature tests passed successfully!")

# def test_sign_then_encrypt():
#     """Test signing and then encrypting with PGP"""
#     pgp = setup().pgp
    
#     # Test key pair - do not use in production
#     private_key = """-----BEGIN PGP PRIVATE KEY BLOCK-----

# lQGVBGf0CAABDAC84Z25L9DpBInabUWNw1EOQKKlHq4/WmvyNvSQyzA6A4b6uJ/p
# EvwN1hN3TWnu9Rj0ktw1l3Qx52ggfOwH2P85a18lyiDOncyiRhxII3Nxds9XrckQ
# EIoCn9zMGC5GWD3d6hRhhr67JJAUyOBAgSZFoQXyA3BVDBNE87cbZdPRRnkUDp3S
# q7zqoMJx97R3yMcxChXrfMZi640V8/eyVHu9W8gOKZEimzSw3tx/mvs9YfusM7TN
# aMWODQyU+FuzFbaHjOhUbKZ6PEofSJenrp9Q9hWKw99iSMlsllGWX7t5yUiB1LOK
# H1n3yW+GkrxnecpdEEY0qsf9qMab/2ArhvHd+yh2D926GjJ1LOlalHOUIbZtH9Mu
# i1GY8eEcTHVafqVxdHDmRPuVJ8O/wjhUke6GoLcUo3f+pGsWCCob43jsvK18FJmB
# 4q1CawqfdzHlCQEbblYxTVUq9V5/Fz1caPuDNEhU8nLjiyMEdKA71oAD0xqc+7f9
# 112lSaFLXvJo8GkAEQEAAf8AZQBHTlUBtAlPbGVrc2FuZHKJAc0EEwEIADgWIQQT
# h9rSHu3Xe+l7cqtKC/75tgCVUgUCZ/QIAAIbAwULCQgHAgYVCgkICwIEFgIDAQIe
# AQIXgAAKCRBKC/75tgCVUiKDC/i29kiO76eqKkPfj+tgwaXrPPpSP0WcLgwEGe/6
# 0mdmQFFWb6kWgYfNfI8vbJJPzsvjfy4vY4TiBNx6ow6lQ3XMQK0OzlHsTUqF7jI2
# yw9WmY0IW4QdreJVhGZnb66gB9+fy8jZcJp52I5Pr5rtrlpO39GpiLGbemueZiMn
# mvcd015jQbXhBe/q8/K0ukSEdD0sm4NT8dJr2wIa740zb+ei1/0ovrF4/oRL2kAH
# majJSEpUabSGfFo6hNUVlL7S2LTJUX8m/cb8F+IjtF29PyZ8dWsYdtN0snlrGKtP
# WO/0DmMkwfkl0zdZybPq+W33Xbg/kcE4E3iK6dl+eODNVRkJptfAlUTuE2b0epyn
# 1ZvixfOJ23kNAFwXw0l9BzlStPDFqXp4Funkq90RWalioY7sYjyPcGfnuH4nmdmW
# MIczKAcqRiWNhujkGUKxOAg01CyNHQUuo+JlAOP9qC7q65sJBWFUT3StIR7FOhRd
# WkFuhkPtlgBcCuPWCAI95LtWmp0DmARn9AkaAQgA3e+l1etUzNX7zYs5NcIYql/r
# lhqg8bgfJg0/qE3VY5Euwq56POfcFpL2Pr6+EUbWVp7MgRSRO0mN8wFXqMxoe3nm
# y9hQTMGPYG7vylEH0RT2AoYGgfrL3s2+d5Yo5MvPRwhbdJUylDfgRLCGjRdx9ktH
# 6/FwF0ZBmjwLx4wgUuDfXDNlsKmexAWYf1lA8tE1GlOndiIUmAqGRPw/edSQdluu
# Dw0tFne2ugVu/Qqxg65Ak3GWjOcgeIMw2xBJi0qwPVe4ehVOPuk2s59RVzirk7bc
# PdOpypT6pk2AredXQVoEqitgvrgKhS9e5n+wV7ta4QTMohNPif89jNPz5zVwqwAR
# AQABAAf/XM9fk+5mV4Sy2Pl8UrlZFEKpo+t926fKSuDmeo0WDB3GJr7NOpQhi0Uo
# +gNNO69aaf3deqttspPnazRoApx8owpO1Hcg2YBmY4bw+zPYp3xzBooixY9qW0HC
# AYOHlWqPw15A1Keh767QOBl0IyOnUj6YFJiybeFBBO1QZcH5MH63PMGXIoj0IOKb
# Iyt/qOwut3gfHLcc2C6J3Yr6xsgLmjZIb0lF3MeuukjV+sB0SLjUscTqHmooiHXb
# CG0fcG9NZk4U0yw6jyyyqmfJ6Sa5GVLKZ+cYnj/G4e5dbaCqTDYdfsY9OYTDbyNO
# e0SRfKA8yeFP7jLKMyeTYffP2ySi5QQA7TQ0pVl7w1WslZaFPn2iLuURld3SIQNz
# WlrKamCJUXTydZ4VuqieRnMSuLimdjusdWkXSKIj0cTUh7Xi62cawmmVAiCVs+YY
# BqQlOVVwVW4rBDYV7BrCpkcC4M1qKb+VXCHGAgq1ljGJjCKh6jXv+GDKt4+sUzAK
# yvmG6HiOs4cEAO+Fujy6D390BSPBBoktSLI/E6rUFkQ6AQkekMnb/Kc+CMGS2m7T
# 5J/KKI4KOzmTLlwOGuGLzR7XylVDAqwGWt7ViRjDLimc9f3/SPU8eEmqtxI40DUn
# LhHLGx95Fbu0LUm1zxqLlR7FKQFM7vfUBArgwRjC7HGv3pNbUJO/k2q9A/9AdXEB
# Gs6hU7TkNOW5Q9gy3tP/k7c6Eh+2LsxskV0T9zTjl164D4O5Rexvr2MPkEOHnPbM
# 5X/wnnV7c2oQcVYuoHydzntTxTNvNA/sCEY1/lyJXYPEO8c9NScT+ca7YEW9zATo
# dZc6/Jf2kA332gs9U/D8EVyFiaJSJzDlOLTz2jz/iQLsBBgBCAAgFiEEE4fa0h7t
# 13vpe3KrSgv++bYAlVIFAmf0CRoCGwIBQAkQSgv++bYAlVLAdCAEGQEIAB0WIQT1
# PL//jUCk3zyjYXtMPKBtGfUipQUCZ/QJGgAKCRBMPKBtGfUipZ5pB/9O6Pl4u58j
# HuLoWy5AzB5Z/feIioZ+Okdgb4cdBQC6gaUzcYoEwhxMatHh1ok+tqxU8N87udTP
# SCk0C3NkJykC/O2P+lIG/weBXodZWL8gbNlriJp8/7LoxUkGz5RKErLfogTBiaT5
# Y2rN09JfzqB1xV4RcqMuOEJxNTgtlxJWEgi87tKDhmV3dCqda4PBn1fywh0Q1ugn
# 2n4zD78J+HUeKattsIV6xIBb+UTwBcfKe4En85sKsdKbZaZm3M4VIRYVfrrUVJ/a
# jEMRd3UL5RZTn3bVua4wGi1U7SuVi+R1hTF8e5NuD/5RgHZ4h1fdMN+j+toi9ked
# eTOP/M5z3ciC7jkMAIHHcUWzHk7IMYV75kGK9TFouXt9I8VIbz3FEV86DzwCYeN4
# g+TTtxcARRHvXA9wXpKCdbm28UTgCySPtaAOH2NoUSv/+QPfLsfhev+uU7j/lLgU
# 9QI6ZWqThxCKCGJ7Ca+KeBmbPzCft5L6/DzZpN6LRVLAoxucteI3PjqlYzv/ZEqy
# PSlA7BMSPzXIJUmJOY38BGRJMGymbvzzlptsSZP95ZN8aUm6EodBDbH7TGpn3F2V
# KMI+PpU/itjudSBRNbAB//8nXn8WHR3FLstDhaeRdDD2LRwHkXP/HllGaCbhEHrQ
# F6N6Aa4hypG03V6gHWz/3i0sLyLlcmQU1DctmHnoaRpunPYWWrdrvPtizBl7g8x9
# TVklRX3S36kFFbr5MWVg++tUt/yFiOvZaDP1dbOpH2a7AiSEcBCiFyBX8GQLEC45
# TsMPPqGldwm4ljmqDdm8RG+85eaNhyagVD+1UyCtjO60X41avKefcttqgFekmjXF
# XUXY17BLAM7eHQG8SQ==
# =BQF9
# -----END PGP PRIVATE KEY BLOCK-----"""

#     public_key = """-----BEGIN PGP PUBLIC KEY BLOCK-----

# mQGNBGf0CAABDAC84Z25L9DpBInabUWNw1EOQKKlHq4/WmvyNvSQyzA6A4b6uJ/p
# EvwN1hN3TWnu9Rj0ktw1l3Qx52ggfOwH2P85a18lyiDOncyiRhxII3Nxds9XrckQ
# EIoCn9zMGC5GWD3d6hRhhr67JJAUyOBAgSZFoQXyA3BVDBNE87cbZdPRRnkUDp3S
# q7zqoMJx97R3yMcxChXrfMZi640V8/eyVHu9W8gOKZEimzSw3tx/mvs9YfusM7TN
# aMWODQyU+FuzFbaHjOhUbKZ6PEofSJenrp9Q9hWKw99iSMlsllGWX7t5yUiB1LOK
# H1n3yW+GkrxnecpdEEY0qsf9qMab/2ArhvHd+yh2D926GjJ1LOlalHOUIbZtH9Mu
# i1GY8eEcTHVafqVxdHDmRPuVJ8O/wjhUke6GoLcUo3f+pGsWCCob43jsvK18FJmB
# 4q1CawqfdzHlCQEbblYxTVUq9V5/Fz1caPuDNEhU8nLjiyMEdKA71oAD0xqc+7f9
# 112lSaFLXvJo8GkAEQEAAbQJT2xla3NhbmRyiQHNBBMBCAA4FiEEE4fa0h7t13vp
# e3KrSgv++bYAlVIFAmf0CAACGwMFCwkIBwIGFQoJCAsCBBYCAwECHgECF4AACgkQ
# Sgv++bYAlVIigwv4tvZIju+nqipD34/rYMGl6zz6Uj9FnC4MBBnv+tJnZkBRVm+p
# FoGHzXyPL2yST87L438uL2OE4gTceqMOpUN1zECtDs5R7E1Khe4yNssPVpmNCFuE
# Ha3iVYRmZ2+uoAffn8vI2XCaediOT6+a7a5aTt/RqYixm3prnmYjJ5r3HdNeY0G1
# 4QXv6vPytLpEhHQ9LJuDU/HSa9sCGu+NM2/notf9KL6xeP6ES9pAB5moyUhKVGm0
# hnxaOoTVFZS+0ti0yVF/Jv3G/BfiI7RdvT8mfHVrGHbTdLJ5axirT1jv9A5jJMH5
# JdM3Wcmz6vlt9124P5HBOBN4iunZfnjgzVUZCabXwJVE7hNm9Hqcp9Wb4sXzidt5
# DQBcF8NJfQc5UrTwxal6eBbp5KvdEVmpYqGO7GI8j3Bn57h+J5nZljCHMygHKkYl
# jYbo5BlCsTgINNQsjR0FLqPiZQDj/agu6uubCQVhVE90rSEexToUXVpBboZD7ZYA
# XArj1ggCPeS7Vpq5AY0EZ/QIAAEMALtLmThKtOcowooQcUmnMiYG8cRmo97EZVnS
# QuhbuWDmdzHuP1MtFxj7CyFxbKryNA0H9OAa79NOVAugejKecq2XMFP0EgHA5ynY
# rpDfk+dKrw0YM6fUoNx0FGu36RliAex3Rrmf4H5ZcVq33gfTO8rwOo4r/MZlvtTl
# 6MXUGfW6mb2mRUUCvVfwlgKpf0Cwwu8rCf3MoC9xNfGbGmfd0D2eVja9YVqS8kwD
# +W4y4OTedpC6jkzoZLeBdgxO9OsJI5hRztWJkntkAWiuppm+IDbiwFcCF7giwl8e
# FejzlcXv8sq+TnVI+jWYyKfRtCuzdR8xab2701NRJAYMa2JjtvMiI+xiQ/fGNlAR
# XyiZ4L6ayDmpAflpAV39ToXPlQX98BIMSfyZrGWYGHtmlL4My9pMRfXYg24bWx4E
# rukizubV/FoiIC2hiwEz+FiS8LliRJq0lp/+I/EoiD3NkJn6W4fZOh5afF9DkmjV
# /GSag46txl2ChEos7mBSOcFZC6ALEwARAQABiQG2BBgBCAAgFiEEE4fa0h7t13vp
# e3KrSgv++bYAlVIFAmf0CAACGwwACgkQSgv++bYAlVKIKAwAiH4b2G3yvto4Jyxx
# qIIsak8DivHCY37xP6qVkyK6vkBfAgjKonc3UM9Yi3MvwGdLjEvrKZ/awLhUyg0j
# YmIHg3yz/2oIoxa2Pq+/LRt80YeqNhccfEJM0ZDDhUcxHOIFkhots8OesWH/wLUX
# DxJcBgt4VbYLHruSM5bw+N2OEtO3isSgMkA+kgC5SyZWyuTtSkJ7DJqjTyA4U3jF
# 4s5PsEUIbQ/6ZfRWzWEypSYmnUytOKSg/t/8p5hZO71P3EIVmdYwh9GMoZqAZgpb
# 1nN14crljTIi4N1HWQs3Amwvs24TiexPxPEhuWEy19iKnTY4XG7kH/sSBoC9yJoa
# lc8ybz5jtdfj7ieiant6Ycak0XoI+Pm1YaMC8JBABxc7yL7n2bGytT/K7YnDBIAM
# /Eo6Mf+r97ikto7K6gHaLNfAI6ZWneWhgKAPQnou20d/bGgiPjpaRD8N01Bpbncq
# x/UfxLWCjomRw0rjpjkoySoVsVdhQtCUXUv3kOHFydyOjSzq
# =LMuI
# -----END PGP PUBLIC KEY BLOCK-----"""

#     # Test message and file name
#     message = bytes("This is a test message for PGP signing and encryption", "utf-8")
#     file_name = "test.txt"
    
#     # Test sign and encrypt with combined operation
#     encrypted_signed = pgp.encrypt(
#         message=message,
#         public_key=public_key,
#         private_key=private_key,
#         hash_algorithm="SHA-256", 
#         algorithm="AES-256",
#         file_name=file_name,
#         armor=True
#     )
    
#     # Should be encrypted (should start with -----BEGIN PGP MESSAGE-----)
#     encrypted_text = str(encrypted_signed, "utf-8")
#     assert_that(encrypted_text).contains("-----BEGIN PGP MESSAGE-----")
    
#     # Decrypt and automatically verify with the private key
#     decrypted = pgp.decrypt(
#         encrypted_message=encrypted_signed,
#         private_key=private_key,
#         verify=True  # Automatically verify the signature
#     )
    
#     # Verify decryption worked
#     assert_that(decrypted).is_equal_to(message)
    
#     print("Sign and encrypt test passed successfully!")

# def _test_all():
#     test_pgp_sign_verify()
#     test_sign_then_encrypt()

# run_test(_test_all)