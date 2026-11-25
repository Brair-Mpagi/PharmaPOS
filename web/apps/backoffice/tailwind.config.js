/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // Coffee/Brown theme - PharmaPOS brand palette
        coffee: {
          50:  '#fdf8f0',
          100: '#f5e6d0',
          200: '#e8c99a',
          300: '#d4a96a',
          400: '#c08840',
          500: '#a0692a',
          600: '#7d4f1e',
          700: '#5c3814',
          800: '#3d230c',
          900: '#1f1006',
        },
        cream: {
          50:  '#fefcf8',
          100: '#fdf7ee',
          200: '#f9edd8',
          300: '#f3deb8',
          400: '#e8c88a',
          500: '#d4a85c',
        },
        mocha: {
          light: '#c9956a',
          DEFAULT: '#8b5e3c',
          dark:  '#5c3818',
        },
        parchment: '#f5ede1',
        espresso: '#2c1a0e',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      backgroundImage: {
        'coffee-gradient': 'linear-gradient(135deg, #5c3818 0%, #8b5e3c 50%, #c9956a 100%)',
        'cream-gradient': 'linear-gradient(180deg, #fdf7ee 0%, #f5ede1 100%)',
      },
    },
  },
  plugins: [],
}
