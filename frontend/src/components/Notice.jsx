export function ErrorNotice({ error }) {
  if (!error) return null;
  const serverMessage = error.response?.data?.message;
  const message = error.response?.status >= 500 && (!serverMessage || serverMessage === 'An unexpected server error occurred')
    ? 'The server could not save this. Check the backend terminal for the error details, then try again.'
    : serverMessage || error.message || 'Something went wrong. Please try again.';
  return <div className="notice error-notice" role="alert">{message}</div>;
}

export function SuccessNotice({ children }) {
  return children ? <div className="notice success-notice" role="status">{children}</div> : null;
}
