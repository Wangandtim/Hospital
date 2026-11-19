Component({
  properties: {
    status: { type: String, value: 'idle' },
    hasMore: { type: Boolean, value: true },
    total: { type: Number, value: 0 },
    error: { type: String, value: '' }
  },
  methods: {
    onRetry() {
      this.triggerEvent('retry');
    }
  }
});
