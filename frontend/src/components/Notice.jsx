export function ErrorNotice({ error }) {
  if (!error) return null;
  const serverMessage = error.response?.data?.message;
  const message = error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT'
    ? 'The server is taking longer than usual to respond. On Render’s free plan it may be waking up; wait a little, then try again.'
    : !error.response && error.message === 'Network Error'
      ? 'Could not reach the BorrowBox server. Check that the backend is available, then try again.'
      : error.response?.status >= 500 && (!serverMessage || serverMessage === 'An unexpected server error occurred')
    ? 'The server could not save this. Check the backend terminal for the error details, then try again.'
    : serverMessage || error.message || 'Something went wrong. Please try again.';
  return <div className="notice error-notice" role="alert">{message}</div>;
}

export function SuccessNotice({ children }) {
  return children ? <div className="notice success-notice" role="status">{children}</div> : null;
}
