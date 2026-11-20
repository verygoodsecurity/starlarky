load("@vgs//browsers", "kitesurf")

CARD_REFERENCES = ["card.number", "card.expiry", "card.security_code"]

def reference_for(field):
    hint = field["autocomplete"]
    label = field["label"].lower()
    if hint == "cc-number" or "card number" in label:
        return "card.number"
    if hint == "cc-exp" or "expiry" in label:
        return "card.expiry"
    if hint == "cc-csc" or "security code" in label:
        return "card.security_code"
    return None

def plan(fields):
    best = {}
    for field in fields:
        reference = reference_for(field)
        if reference and field["origin"] == "https://checkoutshopper-test.adyen.com":
            score = 2 if field["label"] else 1
            previous = best.get(reference, (0, []))
            if score > previous[0]:
                best[reference] = (score, [field["handle"]])
            elif score == previous[0]:
                previous[1].append(field["handle"])
    return {reference: handles[0] for reference, (_, handles) in best.items() if len(handles) == 1}

def checkout():
    kitesurf.open(start_url)
    for _ in range(20):
        current = plan(kitesurf.inspect()["fields"])
        if sorted(current.keys()) == sorted(CARD_REFERENCES):
            break
    if sorted(current.keys()) != sorted(CARD_REFERENCES):
        fail("the embedded browser did not discover the three Adyen Secured Fields")
    for reference in CARD_REFERENCES:
        kitesurf.fill(current[reference], reference)

    if not kitesurf.wait_for_text("#state", "Card fields encrypted"):
        fail("Adyen did not report that the test card was ready")

    for action in kitesurf.inspect()["actions"]:
        if action["label"].lower().startswith("pay"):
            kitesurf.click(action["handle"])
            if not kitesurf.wait_for_text("#result", "EncryptedFieldsReceived"):
                fail("the loopback merchant did not confirm the encrypted fields")
            return
    fail("the embedded browser did not discover the Pay action")

checkout()
