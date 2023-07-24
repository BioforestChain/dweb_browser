/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{vue,ts}"],
  theme: {
    extend: {
      animation: {
        "slow-bounce": "slow-bounce 1s infinite",
        "app-pulse": "app-pulse 1s infinite",
      },
      keyframes: {
        "slow-bounce": {
          "50%": {
            transform: "translateY(-10%)",
            "animation-timing-function": "cubic-bezier(0.8,0,1,1)",
          },
          "0%,100%": {
            transform: "none",
            "animation-timing-function": "cubic-bezier(0, 0, 0.2, 1)",
          },
        },
        "app-pulse": {
          "50%": {
            transform: "scale(0.9)",
          },
        },
        "slow-bounce-1": {
          "25%": {
            transform: "translateY(-18%)",
            "animation-timing-function": "cubic-bezier(0.8,0,1,1)",
          },
          "75%": {
            transform: "translateY(-25%)",
            "animation-timing-function": "cubic-bezier(0.8,0,1,1)",
          },
          "0%,50%,100%": {
            transform: "none",
            "animation-timing-function": "cubic-bezier(0, 0, 0.2, 1)",
          },
        },
      },
    },
  },
  plugins: [require("daisyui")],
  daisyui: {
    themes: [
      "light",
      "dark",
      "cupcake",
      {
        desktop: {
          "color-scheme": "light",
          primary: "#06b6d4",
          secondary: "#c4b5fd",
          accent: "#2dd4bf",
          neutral: "#2b3440",
          info: "#a5f3fc",
          success: "#a7f3d0",
          warning: "#fde68a",
          error: "#fecaca",
          "base-100": "#faf7f5",
          "base-200": "#efeae6",
          "base-300": "#e7e2df",
          "base-content": "#291334",

          "--rounded-btn": "1.9rem",
          "--tab-border": "2px",
          "--tab-radius": ".5rem",
        },
      },
    ],
    darkTheme: "dark",
    base: true, // applies background color and foreground color for root element by default
    styled: true, // include daisyUI colors and design decisions for all components
    utils: true, // adds responsive and modifier utility classes
    rtl: false, // rotate style direction from left-to-right to right-to-left. You also need to add dir="rtl" to your html tag and install `tailwindcss-flip` plugin for Tailwind CSS.
    prefix: "", // prefix for daisyUI classnames (components, modifiers and responsive class names. Not colors)
    logs: false, // Shows info about daisyUI version and used config in the console when building your CSS
  },
};
