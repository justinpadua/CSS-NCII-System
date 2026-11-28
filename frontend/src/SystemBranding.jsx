const brandLogos = [
  { src: '/assets/system-logo-1.png', alt: 'Department of Education and Bagong Pilipinas' },
  { src: '/assets/system-logo-2.png', alt: 'Judge Jose de Venecia Sr. Technical-Vocational Secondary School' },
  { src: '/assets/system-logo-3.png', alt: 'Department of Education Dagupan City' },
]

function SystemBrandingLayout({ children, headerContent, studentMode = false, adminMode = false }) {
  return (
    <div className={studentMode ? 'flex h-dvh min-h-0 flex-col overflow-hidden' : 'flex min-h-svh flex-col'}>
      <header className={studentMode
        ? 'z-40 flex h-[76px] min-h-[76px] shrink-0 flex-nowrap items-center gap-x-2 border-b border-cyan-100/10 bg-[#071722] px-2 py-2 shadow-lg shadow-slate-950/20 sm:h-[72px] sm:min-h-[72px] sm:gap-x-3 sm:px-5'
        : adminMode
          ? 'sticky top-0 z-40 flex min-h-[112px] flex-wrap items-center gap-x-3 gap-y-1 border-b border-slate-200 bg-white px-3 py-3 sm:min-h-[72px] sm:flex-nowrap sm:px-5 sm:py-2'
          : 'sticky top-0 z-40 flex min-h-[112px] flex-wrap items-center gap-x-3 gap-y-1 border-b border-slate-200 bg-white/95 px-3 py-3 shadow-sm backdrop-blur sm:min-h-[72px] sm:flex-nowrap sm:px-5 sm:py-2'}>
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
        <div className={studentMode ? 'min-w-0 flex-1' : 'min-w-0 flex-1 basis-full sm:basis-auto'}>
          {headerContent}
        </div>
      </header>
      <div className={studentMode ? 'relative min-h-0 flex-1 overflow-hidden' : 'relative flex-1 pb-10'}>{children}</div>
      <footer className={studentMode
        ? 'pointer-events-none fixed bottom-2 right-3 z-50 rounded-md px-2 py-1 text-right text-[10px] font-medium text-slate-300/75 sm:bottom-3 sm:right-4 sm:text-[11px]'
        : adminMode
          ? 'pointer-events-none fixed bottom-2 right-2 z-50 px-2 py-1 text-right text-[11px] font-medium text-slate-500 sm:bottom-3 sm:right-4'
          : 'pointer-events-none fixed bottom-2 right-2 z-50 rounded-md bg-white/85 px-2 py-1 text-right text-[11px] font-medium text-slate-600 shadow-sm backdrop-blur sm:bottom-3 sm:right-4'}>
        Developed by JL
      </footer>
    </div>
  )
}

export default SystemBrandingLayout
