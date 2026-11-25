import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'PharmaPOS — Back Office',
  description: 'Enterprise Pharmacy Management System',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
