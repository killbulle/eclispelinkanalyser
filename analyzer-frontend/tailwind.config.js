/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                primary: {
                    50: '#eff6ff',
                    100: '#dbeafe',
                    200: '#bfdbfe',
                    300: '#93c5fd',
                    400: '#60a5fa',
                    500: '#3b82f6',
                    600: '#2563eb',
                    700: '#1d4ed8',
                    800: '#1e40af',
                    900: '#1e3a8a',
                    DEFAULT: '#3b82f6',
                    hover: '#2563eb',
                },
                background: {
                    light: '#f8fafc',
                    card: '#ffffff',
                    sidebar: '#f9fafb',
                    header: '#f1f5f9',
                },
                border: {
                    light: '#e2e8f0',
                    DEFAULT: '#cbd5e1',
                    dark: '#94a3b8',
                },
                text: {
                    primary: '#0f172a',
                    secondary: '#334155',
                    muted: '#64748b',
                    subtle: '#94a3b8',
                },
                status: {
                    error: {
                        50: '#fef2f2',
                        100: '#fee2e2',
                        500: '#ef4444',
                        DEFAULT: '#ef4444',
                    },
                    warning: {
                        50: '#fffbeb',
                        100: '#fef3c7',
                        500: '#f59e0b',
                        DEFAULT: '#f59e0b',
                    },
                    info: {
                        50: '#eff6ff',
                        100: '#dbeafe',
                        500: '#3b82f6',
                        DEFAULT: '#3b82f6',
                    },
                    success: {
                        50: '#f0fdf4',
                        100: '#dcfce7',
                        500: '#10b981',
                        DEFAULT: '#10b981',
                    }
                },
                neutral: {
                    50: '#f8fafc',
                    100: '#f1f5f9',
                    200: '#e2e8f0',
                    300: '#cbd5e1',
                    400: '#94a3b8',
                    500: '#64748b',
                    600: '#475569',
                    700: '#334155',
                    800: '#1e293b',
                    900: '#0f172a',
                }
            },
            fontFamily: {
                sans: ['Inter', 'sans-serif'],
                mono: ['JetBrains Mono', 'monospace'],
            },
            fontSize: {
                '2xs': '0.625rem',     // 10px
                'xs': '0.75rem',       // 12px
                'sm': '0.875rem',      // 14px
                'base': '1rem',        // 16px
                'lg': '1.125rem',      // 18px
                'xl': '1.25rem',       // 20px
                '2xl': '1.5rem',       // 24px
            },
            spacing: {
                'xs': '0.25rem',       // 4px
                'sm': '0.5rem',        // 8px
                'md': '0.75rem',       // 12px
                'base': '1rem',        // 16px
                'lg': '1.5rem',        // 24px
                'xl': '2rem',          // 32px
                '2xl': '3rem',         // 48px
            },
            borderRadius: {
                'sm': '0.25rem',       // 4px
                'DEFAULT': '0.375rem', // 6px
                'md': '0.5rem',        // 8px
                'lg': '0.75rem',       // 12px
            },
            boxShadow: {
                'sm': '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
                'DEFAULT': '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
                'md': '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)',
                'lg': '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)',
                'card': '0 2px 8px rgba(0, 0, 0, 0.08)',
                'sidebar': '-2px 0 10px rgba(0, 0, 0, 0.05)',
                'node': '0 2px 8px rgba(0, 0, 0, 0.08)',
            },
        },
    },
    plugins: [],
}
