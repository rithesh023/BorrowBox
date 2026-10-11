import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Navbar() {
  const { user, adminSession, logout } = useAuth();
  return <header className="navbar">
    <Link to="/" className="brand"><span className="brand-mark">b</span>borrowbox<span className="brand-period">.</span></Link>
    <nav className="main-nav"><NavLink to="/items">Explore</NavLink><a href="/#how-it-works">How it works</a>{user && <NavLink to="/dashboard">My dashboard</NavLink>}<NavLink to={adminSession ? '/admin' : '/admin/login'}>{adminSession ? 'Admin dashboard' : 'Admin sign in'}</NavLink></nav>
    <div className="nav-actions">{user ? <><span className="nav-user">Hi, {user.name.split(' ')[0]}</span><button className="login" onClick={logout}>Log out</button><Link className="signup" to="/items/new">List an item <span>↗</span></Link></> : <><Link className="login" to="/login">Log in</Link><Link className="signup" to="/register">Get started <span>↗</span></Link></>}</div>
  </header>;
}
