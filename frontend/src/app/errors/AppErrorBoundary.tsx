import { Component, type ErrorInfo, type ReactNode } from 'react';

interface State {
  hasError: boolean;
}

/**
 * Last line of defence for errors outside the router (providers). Deliberately free of Mantine:
 * the failure may come from the UI library itself.
 */
export class AppErrorBoundary extends Component<{ children: ReactNode }, State> {
  override state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  override componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled application error', error, info.componentStack);
  }

  override render() {
    if (this.state.hasError) {
      return (
        <main style={{ fontFamily: 'system-ui, sans-serif', padding: '3rem', textAlign: 'center' }}>
          <h1>Algo deu errado</h1>
          <p>A aplicação encontrou um erro inesperado. Recarregue a página para continuar.</p>
          <button type="button" onClick={() => window.location.reload()}>
            Recarregar
          </button>
        </main>
      );
    }
    return this.props.children;
  }
}
