/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          200: '#bae6fd',
          300: '#7dd3fc',
          400: '#38bdf8',
          500: '#0ea5e9',
          600: '#0284c7', // Primary blue
          700: '#0369a1',
          800: '#075985',
          900: '#0c4a6e',
          950: '#082f49',
        },
        success: '#22c55e', // green-500
        warning: '#f59e0b', // amber-500
        danger: '#ef4444',  // red-500
      },
      fontFamily: {
        sans: ['Inter', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
      },
      borderRadius: {
        'lg': '0.5rem',   // Inputs
        'xl': '0.75rem',  // Buttons
        '2xl': '1rem',    // Cards
      },
      keyframes: {
        'fade-in-up': {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'float-slow': {
          '0%, 100%': { transform: 'translateY(0) scale(1)' },
          '50%': { transform: 'translateY(-20px) scale(1.05)' },
        },
        'float-slow-reverse': {
          '0%, 100%': { transform: 'translateY(0) rotate(12deg)' },
          '50%': { transform: 'translateY(15px) rotate(15deg)' },
        }
      },
      animation: {
        'fade-in-up': 'fade-in-up 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards',
        'float-slow': 'float-slow 8s ease-in-out infinite',
        'float-slow-reverse': 'float-slow-reverse 10s ease-in-out infinite',
      }
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}
