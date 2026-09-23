import { isApiError } from '@/shared/api/api-error';

export type OperationCommand = 'settle' | 'cancel';

export interface CommandErrorExplanation {
  title: string;
  /** `undefined` keeps the detail written by the server. */
  message?: string;
  /** The operation changed on the server: the screen must show the fresh version. */
  refetch: boolean;
}

const ACTION: Record<OperationCommand, string> = { settle: 'liquidar', cancel: 'cancelar' };

/**
 * Operator-facing explanation of a rejected settlement/cancellation. Branches on the stable error
 * code (optimistic locking and state machine outcomes), never on the server message.
 */
export function explainCommandError(
  error: unknown,
  command: OperationCommand,
): CommandErrorExplanation {
  if (!isApiError(error)) {
    return { title: `Não foi possível ${ACTION[command]} a operação`, refetch: false };
  }
  switch (error.code) {
    case 'PRECONDITION_FAILED':
      return {
        title: 'Operação alterada por outra pessoa',
        message:
          'A operação mudou desde que foi carregada. Os dados atualizados já estão na tela: revise antes de tentar novamente.',
        refetch: true,
      };
    case 'OPERATION_ALREADY_SETTLED':
      return {
        title: 'Operação já liquidada',
        message: 'Esta operação já foi liquidada anteriormente; nenhum novo débito foi realizado.',
        refetch: true,
      };
    case 'INVALID_STATE_TRANSITION':
      return { title: 'Ação não permitida no estado atual', refetch: true };
    case 'CONCURRENT_MODIFICATION':
      return {
        title: 'Conflito de concorrência',
        message:
          'Outra liquidação estava em andamento ao mesmo tempo. Aguarde alguns instantes e tente novamente.',
        refetch: true,
      };
    case 'PRECONDITION_REQUIRED':
      return {
        title: 'Versão da operação não informada',
        message: 'Recarregue a operação e tente novamente.',
        refetch: true,
      };
    case 'INSUFFICIENT_FUNDS':
      return { title: 'Saldo insuficiente no caixa do fundo', refetch: false };
    case 'RESOURCE_NOT_FOUND':
      return { title: 'Operação não encontrada', refetch: false };
    case 'NETWORK_ERROR':
      return {
        title: 'Falha de comunicação',
        message:
          'Não foi possível confirmar o resultado. Os dados serão recarregados para verificar o estado atual da operação.',
        refetch: true,
      };
    default:
      return { title: error.title, refetch: false };
  }
}
