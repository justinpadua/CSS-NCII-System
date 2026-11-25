const brandLogos = [
  { src: '/assets/system-logo-1.png', alt: 'Department of Education and Bagong Pilipinas' },
  { src: '/assets/system-logo-2.png', alt: 'Judge Jose de Venecia Sr. Technical-Vocational Secondary School' },
  { src: '/assets/system-logo-3.png', alt: 'Department of Education Dagupan City' },
]

function SystemBrandingLayout({ children, headerContent }) {
  return (
    <div className="flex min-h-svh flex-col">
      <header className="sticky top-0 z-40 flex min-h-[112px] flex-wrap items-center gap-x-3 gap-y-1 border-b border-slate-200 bg-white/95 px-3 py-3 shadow-sm backdrop-blur sm:min-h-[72px] sm:flex-nowrap sm:px-5 sm:py-2">
        <div className="flex shrink-0 items-center gap-2 sm:gap-2.5" aria-label="CSS NC II institutional logos">
          {brandLogos.map(logo => (
            <img
              key={logo.src}
              src={logo.src}
              alt={logo.alt}
              className="h-8 w-auto object-contain sm:h-9"
            />
          ))}
        </div>
        <div className="min-w-0 flex-1 basis-full sm:basis-auto">
          {headerContent}
        </div>
      </header>
      <div className="relative flex-1 pb-10">{children}</div>
      <footer className="pointer-events-none fixed bottom-2 right-2 z-50 rounded-md bg-white/85 px-2 py-1 text-right text-[11px] font-medium text-slate-600 shadow-sm backdrop-blur sm:bottom-3 sm:right-4">
        Developed by JL
      </footer>
    </div>
  )
}

export default SystemBrandingLayout
