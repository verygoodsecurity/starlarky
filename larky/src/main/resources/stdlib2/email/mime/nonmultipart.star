def MIMENonMultipart(MIMEBase):
    """
    Base class for MIME non-multipart type messages.
    """
    def attach(self, payload):
        """
         The public API prohibits attaching multiple subparts to MIMEBase
         derived subtypes since none of them are, by definition, of content
         type multipart/*

        """
