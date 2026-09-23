import { useEffect, useState } from 'react';

/**
 * Returns `value` once it has stopped changing for `delay` ms.
 *
 * Values are compared by their JSON serialization, not by reference: form values and query
 * params are rebuilt as new objects on every render, and a reference comparison would restart the
 * timer forever (and re-render in a loop). Only use it with JSON-serializable values.
 */
export function useDebouncedValue<T>(value: T, delay: number): T {
  const key = JSON.stringify(value);
  const [debounced, setDebounced] = useState<{ key: string; value: T }>({ key, value });

  useEffect(() => {
    if (key === debounced.key) return undefined;
    const timer = setTimeout(() => {
      setDebounced({ key, value: JSON.parse(key) as T });
    }, delay);
    return () => {
      clearTimeout(timer);
    };
  }, [key, delay, debounced.key]);

  return debounced.value;
}
