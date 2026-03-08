import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Navbar } from "@/components/shared";
import { LanguageProvider } from "@/components/shared/LanguageProvider";
import { SpaceBackdrop } from "@/components/shared/SpaceBackdrop";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "SARATHI - Emergency Navigation System",
  description: "AI-powered emergency response platform for ambulance, police, and fire services",
  viewport: "width=device-width, initial-scale=1",
  icons: {
    icon: '/logo.png',
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <head>
        <script
          async
          src={`https://maps.googleapis.com/maps/api/js?key=${process.env.NEXT_PUBLIC_GOOGLE_MAPS_KEY}&libraries=places,routes,marker`}
        />
        <meta name="theme-color" content="#991b1b" />
      </head>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-gray-950 text-white overflow-x-hidden`}
      >
        <LanguageProvider>
          <SpaceBackdrop />
          <div className="relative z-10">
            <Navbar />
            {children}
          </div>
        </LanguageProvider>
      </body>
    </html>
  );
}
