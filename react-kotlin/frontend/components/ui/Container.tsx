import { ReactNode } from 'react'

interface ContainerProps {
  children: ReactNode
  className?: string
}

export function Container({ children, className = '' }: ContainerProps) {
  return (
    <div
      className={`flex flex-col bg-surface rounded-xl shadow-[0_0.4rem_3rem_rgba(0,0,0,0.4)] p-10 m-4 w-96 border border-line-subtle ${className}`}
    >
      {children}
    </div>
  )
}
