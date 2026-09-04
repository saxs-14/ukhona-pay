/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        // Warm terracotta - the brand/primary color. Evokes Lowveld red soil,
        // deliberately chosen over the default Tailwind blue-500 SaaS look.
        terracotta: {
          50: "#FDF3EC",
          100: "#FBE4D3",
          200: "#F5C6A3",
          300: "#EDA26D",
          400: "#E17F42",
          500: "#C96328",
          600: "#A94F1E",
          700: "#883E19",
          800: "#6E3317",
          900: "#5B2B16",
        },
        // Bushveld green - success, cashback, verified states.
        bushveld: {
          50: "#F0FAF0",
          100: "#DBF1DB",
          200: "#B6E3B8",
          300: "#86CC8D",
          400: "#57AF62",
          500: "#2F8F4E",
          600: "#227240",
          700: "#1C5B35",
          800: "#18492C",
          900: "#143C25",
        },
        // Sand - warm neutral scale replacing cold slate/gray.
        sand: {
          50: "#FEFCF8",
          100: "#FBF6EC",
          200: "#F3E9D6",
          300: "#E4D3B4",
          400: "#C7AE88",
          500: "#A88A63",
          600: "#8A6E4C",
          700: "#6E573C",
          800: "#4A3B2A",
          900: "#2E241A",
        },
        // Gold - ratings, highlights, ATM/cash accents.
        gold: {
          400: "#F2B441",
          500: "#E2971E",
          600: "#C17A12",
        },
      },
      fontFamily: {
        display: ["Calistoga", "ui-serif", "Georgia", "serif"],
        sans: ["Inter", "ui-sans-serif", "system-ui", "-apple-system", "sans-serif"],
      },
      boxShadow: {
        warm: "0 4px 16px -4px rgba(91, 43, 22, 0.18)",
        "warm-lg": "0 12px 32px -8px rgba(91, 43, 22, 0.22)",
      },
      borderRadius: {
        xl2: "1.25rem",
      },
    },
  },
  plugins: [],
};
