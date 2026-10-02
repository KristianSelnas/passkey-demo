import { InputHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  className?: string
}

export function Input({ className = '', ...props }: InputProps) {
  return (
    <input
      className={`
        w-full px-3 py-2.5 border border-line
        rounded-lg text-base mb-4
        bg-canvas text-fg
        focus:outline-none focus:border-brand-light
        focus:ring-4 focus:ring-brand-light/20
        placeholder:text-subtle
        ${className}
      `}
      {...props}
    />
  )
}
