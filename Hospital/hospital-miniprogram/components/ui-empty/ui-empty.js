Component({
  properties: {
    title: { type: String, value: '这里还空着' },
    hint: { type: String, value: '' },
    mark: { type: String, value: '空' },
    actionText: { type: String, value: '' }
  },
  methods: {
    onAction() {
      this.triggerEvent('action');
    }
  }
});
