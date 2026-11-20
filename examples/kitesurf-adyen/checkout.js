import { AdyenCheckout, Card } from '@adyen/adyen-web';

const result = document.getElementById('result');

async function mountCheckout() {
  const checkout = await AdyenCheckout({
    environment: 'test',
    clientKey: window.__ADYEN_CONFIG__.clientKey,
    countryCode: 'NL',
    paymentMethodsResponse: {
      paymentMethods: [{ name: 'Cards', type: 'scheme', brands: ['visa', 'mc'] }],
    },
    onSubmit: async (state, component) => {
      const response = await fetch('/api/payments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ paymentMethod: state.data.paymentMethod }),
      });
      result.textContent = JSON.stringify(await response.json());
      component.setStatus(response.ok ? 'success' : 'error');
    },
  });
  new Card(checkout, {
    showPayButton: true,
    onChange(state) {
      result.dataset.ready = String(state.isValid);
    },
  }).mount('#card-container');
}

mountCheckout().catch(() => {
  result.textContent = 'Checkout setup failed';
});
