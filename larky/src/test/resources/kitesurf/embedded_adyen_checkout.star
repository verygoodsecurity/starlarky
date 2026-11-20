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
                best[reference] = (score, [field])
            elif score == previous[0]:
                previous[1].append(field)
    return {reference: matches[0] for reference, (_, matches) in best.items() if len(matches) == 1}

def checkout():
    kitesurf.open(start_url)
    for _ in range(20):
        current = plan(kitesurf.inspect()["fields"])
        if sorted(current.keys()) == sorted(CARD_REFERENCES):
            break
    if sorted(current.keys()) != sorted(CARD_REFERENCES):
        fail("the embedded browser did not discover the three Adyen Secured Fields")
    for reference in CARD_REFERENCES:
        field = current[reference]
        kitesurf.fill(field["handle"], reference)

    if not kitesurf.wait_for_text("[data-checkout-state]", "Card fields encrypted"):
        fail("Adyen did not report that the test card was ready")

    for action in kitesurf.inspect()["actions"]:
        if action["label"].lower().startswith("pay"):
            kitesurf.click(action["handle"])
            if not kitesurf.wait_for_text("[data-checkout-result]", "Checkout accepted"):
                fail("the sandbox checkout did not accept the encrypted fields")
            return [(reference, current[reference]["label"], current[reference]["origin"])
                    for reference in CARD_REFERENCES]
    fail("the embedded browser did not discover the Pay action")

discovered_fields = checkout()
