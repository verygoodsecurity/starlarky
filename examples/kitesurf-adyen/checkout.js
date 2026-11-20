import { AdyenCheckout, Card, Dropin } from '@adyen/adyen-web';

const result = document.getElementById('result');

async function mountCheckout() {
  const dropinMount = document.getElementById('payment-methods');
  const checkoutStatus = document.querySelector('[data-checkout-state]');
  const cardOptions = {
    onChange(state) {
      checkoutStatus.textContent = state.isValid ? 'Card fields encrypted' : '';
    },
  };
  const checkout = await AdyenCheckout({
    environment: 'test',
    clientKey: window.__ADYEN_CONFIG__.clientKey,
    countryCode: 'NL',
    locale: 'en-US',
    amount: { currency: 'EUR', value: 1000 },
    paymentMethodsResponse: {
      paymentMethods: [{ name: 'Cards', type: 'scheme', brands: ['visa', 'mc'] }],
    },
    onSubmit: async (state, component, actions) => {
      const response = await fetch('/api/payments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          paymentMethod: state.data.paymentMethod,
          browserInfo: state.data.browserInfo,
        }),
      });
      const receipt = await response.json();
      const accepted = receipt.resultCode === 'Authorised'
          || receipt.resultCode === 'EncryptedFieldsReceived';
      result.textContent = (accepted ? 'Checkout accepted. ' : '') + JSON.stringify(receipt);
      if (actions) {
        if (receipt.resultCode === 'Authorised') actions.resolve({ resultCode: receipt.resultCode });
        else actions.reject();
      } else {
        component.setStatus(accepted ? 'success' : 'error');
      }
    },
  });
  if (dropinMount) {
    new Dropin(checkout, {
      paymentMethodComponents: [Card],
      paymentMethodsConfiguration: { card: cardOptions },
      openFirstPaymentMethod: true,
    }).mount(dropinMount);
  } else {
    new Card(checkout, { ...cardOptions, showPayButton: true }).mount('#card-container');
  }
}

mountCheckout().catch(() => {
  result.textContent = 'Checkout setup failed';
});
