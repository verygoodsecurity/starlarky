package com.verygood.security.larky.modules.openssl;

import java.math.BigInteger;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class LarkyX509Cert {
  BigInteger gmtime_adj_notAfter;
  BigInteger gmtime_adj_notBefore;
  String issuer_name;
  String subject_name;
}
