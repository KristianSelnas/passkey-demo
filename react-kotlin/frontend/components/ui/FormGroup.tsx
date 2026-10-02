import { ReactNode } from 'react'

interface FormGroupProps {
  label: string
  children: ReactNode
  htmlFor?: string
}

export function FormGroup({ label, children, htmlFor }: FormGroupProps) {
  return (
    <div className="flex flex-col">
      <label
        htmlFor={htmlFor}
        className="block font-semibold mb-2 text-fg-label"
      >
        {label}
      </label>
      {children}
    </div>
  )
}
