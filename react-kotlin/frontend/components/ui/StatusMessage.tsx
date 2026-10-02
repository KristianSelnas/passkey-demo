interface StatusMessageProps {
  type: 'success' | 'error' | 'info'
  message: string
  className?: string
}

export function StatusMessage({ type, message, className = '' }: StatusMessageProps) {
  if (!message) return null

  const styles = {
    success: 'bg-success-bg text-success border-success-border',
    error: 'bg-error-bg text-error border-error-border',
    info: 'bg-info-bg text-info border-info-border',
  }

  return (
    <div className={`
      mt-6 p-4 rounded-lg border
      ${styles[type]}
      ${className}
    `}>
      {message}
    </div>
  )
}
