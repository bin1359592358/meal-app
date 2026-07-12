Component({
  properties: {
    icon: {
      type: String,
      value: '📭',
    },
    text: {
      type: String,
      value: '',
    },
    buttonText: {
      type: String,
      value: '',
    },
  },

  methods: {
    onAction() {
      this.triggerEvent('onAction')
    },
  },
})
