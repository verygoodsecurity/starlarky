def BaseSubprocessTransport(transports.SubprocessTransport):
    """
     Create the child process: set the _proc attribute

    """
    def __repr__(self):
        """
        'closed'
        """
    def _start(self, args, shell, stdin, stdout, stderr, bufsize, **kwargs):
        """
         has the child process finished?

        """
    def __del__(self, _warn=warnings.warn):
        """
        f"unclosed transport {self!r}
        """
    def get_pid(self):
        """
        '%r exited with return code %r'
        """
    async def _wait(self):
            """
            Wait until the process exit and return the process return code.

                    This method is a coroutine.
            """
    def _try_finish(self):
        """
        f'<{self.__class__.__name__} fd={self.fd} pipe={self.pipe!r}>'
        """
    def connection_lost(self, exc):
