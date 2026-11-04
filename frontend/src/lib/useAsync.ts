import { useCallback, useEffect, useState } from 'react';

/** Loading / error / data for one async source, with reload. */
export function useAsync<T>(load: () => Promise<T>, deps: unknown[]) {
  const [state, setState] = useState<{ loading: boolean; error: unknown; data: T | null }>({
    loading: true,
    error: null,
    data: null,
  });

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(load, deps);

  const reload = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: null }));
    run()
      .then((data) => setState({ loading: false, error: null, data }))
      .catch((error: unknown) => setState({ loading: false, error, data: null }));
  }, [run]);

  useEffect(() => {
    let active = true;
    setState((s) => ({ ...s, loading: true, error: null }));
    run()
      .then((data) => active && setState({ loading: false, error: null, data }))
      .catch((error: unknown) => active && setState({ loading: false, error, data: null }));
    return () => {
      active = false;
    };
  }, [run]);

  return { ...state, reload, setData: (data: T) => setState({ loading: false, error: null, data }) };
}
