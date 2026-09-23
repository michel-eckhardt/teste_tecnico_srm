import '@tanstack/react-table';

declare module '@tanstack/react-table' {
  // Signature fixed by the library (TData/TValue unused here).
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface ColumnMeta<TData, TValue> {
    /** Numeric columns are right-aligned. */
    align?: 'right';
  }
}
