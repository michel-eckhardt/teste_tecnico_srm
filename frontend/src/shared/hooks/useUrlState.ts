import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router';

export interface UrlStateCodec<T> {
  /** Reads (and validates) the state from the URL; must never throw. */
  parse: (params: URLSearchParams) => T;
  /** Writes the state; keys it leaves out are removed from the URL. */
  serialize: (state: T) => Record<string, string>;
  /** Keys owned by this state (other search params are preserved). */
  keys: readonly string[];
}

/**
 * State that lives in the URL search params: shareable, bookmarkable and restored by the back
 * button. Each change creates a history entry unless `replace` is requested.
 */
export function useUrlState<T>(codec: UrlStateCodec<T>) {
  const [searchParams, setSearchParams] = useSearchParams();
  const serialized = searchParams.toString();
  const { parse, serialize, keys } = codec;

  const state = useMemo(() => parse(new URLSearchParams(serialized)), [parse, serialized]);

  const setState = useCallback(
    (update: (current: T) => T, options: { replace?: boolean } = {}) => {
      setSearchParams(
        (current) => {
          const next = new URLSearchParams(current);
          for (const key of keys) next.delete(key);
          for (const [key, value] of Object.entries(serialize(update(parse(current))))) {
            next.set(key, value);
          }
          return next;
        },
        { replace: options.replace ?? false },
      );
    },
    [keys, parse, serialize, setSearchParams],
  );

  return [state, setState] as const;
}
