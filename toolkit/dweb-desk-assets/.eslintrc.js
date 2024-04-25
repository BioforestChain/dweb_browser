module.exports = {
  root: true,
  env: {
    node: true,
  },
  extends: [
    //
    "plugin:vue/vue3-essential",
    "eslint:recommended",
    "@vue/eslint-config-typescript",
  ],
  rules: {
    //
    "vue/multi-word-component-names": "off",
    "vue/valid-template-root": "off",
    "no-unused-vars": "off",
    "no-undef": "off",
  },
};
