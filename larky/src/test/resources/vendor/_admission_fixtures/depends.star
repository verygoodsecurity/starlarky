# Loads a module the cache declines, so is declined too (DEPENDS_ON).
load("@vendor//_admission_fixtures/host_service", "ping")

def call():
    return ping()
