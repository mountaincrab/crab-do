interface Props {
  loading: boolean
  hasMore: boolean
  error?: string
  loadMore: () => void
  retry: () => void
}

export default function LoadMore({ loading, hasMore, error, loadMore, retry }: Props) {
  return (
    <div className="text-center text-sm text-fg-muted py-2 shrink-0" aria-live="polite">
      {error ? (
        <><p role="alert">Couldn’t load items.</p><button className="text-accent-text p-2" onClick={retry}>Retry</button></>
      ) : loading ? <span>Loading…</span> : hasMore ? (
        <button className="text-accent-text p-2" onClick={loadMore}>Load more</button>
      ) : null}
    </div>
  )
}
