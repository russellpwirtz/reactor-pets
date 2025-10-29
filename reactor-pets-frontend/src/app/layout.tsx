import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import { QueryProvider } from '@/providers/query-provider';
import { Toaster } from '@/components/ui/toaster';
import { NavBar } from '@/components/layout/nav-bar';
import { UpdateIndicator } from '@/components/ui/update-indicator';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'Reactor Pets',
  description: 'Virtual pet with event sourcing and reactive systems',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <QueryProvider>
          <div className="min-h-screen flex flex-col">
            <NavBar />
            <main className="flex-1">{children}</main>
            <UpdateIndicator />
            <Toaster />
          </div>
        </QueryProvider>
      </body>
    </html>
  );
}
