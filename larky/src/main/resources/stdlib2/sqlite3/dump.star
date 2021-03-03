def _iterdump(connection):
    """

        Returns an iterator to the dump of the database in an SQL text format.

        Used to produce an SQL dump of the database.  Useful to save an in-memory
        database for later restoration.  This function should not be called
        directly but instead called from the Connection method, iterdump().
    
    """
