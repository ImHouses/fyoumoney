import type { ReactNode } from 'react';
import './Layout.css';

interface LayoutProps {
  sidebar: ReactNode;
  children: ReactNode;
}

export function Layout({ sidebar, children }: LayoutProps) {
  return (
    <div className="layout">
      <aside className="sidebar">{sidebar}</aside>
      <main className="main">{children}</main>
    </div>
  );
}
