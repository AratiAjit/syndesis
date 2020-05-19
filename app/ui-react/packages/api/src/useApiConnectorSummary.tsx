import { IApiSummarySoap } from '@syndesis/models';
import * as React from 'react';
import { ApiContext } from './ApiContext';
import { callFetch } from './callFetch';

export function useApiConnectorSummary(
  specification: string,
  connectorTemplateId?: string,
  portName?: string,
  serviceName?: string
) {
  const apiContext = React.useContext(ApiContext);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<false | Error>(false);
  const [apiSummary, setApiSummary] = React.useState<
    IApiSummarySoap | undefined
  >(undefined);

  React.useEffect(() => {
    if (!specification) {
      return;
    }
    const fetchSummary = async () => {
      setLoading(true);
      try {
        const response = await callFetch({
          body: {
            configuredProperties: {
              portName,
              serviceName,
              specification,
            },
            connectorTemplateId: connectorTemplateId
              ? connectorTemplateId
              : 'swagger-connector-template',
          },
          headers: apiContext.headers,
          includeAccept: true,
          includeContentType: true,
          method: 'POST',
          url: `${apiContext.apiUri}/connectors/custom/info`,
        });
        const summary = await response.json();
        if (summary.errorCode) {
          throw new Error(summary.userMsg);
        }
        if (!summary.actionsSummary) {
          let errorMessage = '';
          // we should be getting an array of error objects
          if (Array.isArray(summary.errors)) {
            errorMessage = summary.errors
              .map((e: string | any) => (e.message ? e.message : e))
              .join('\n');
          } else {
            // but in case we don't, let's show what we got and hope for the best
            errorMessage = JSON.stringify(summary);
          }
          throw new Error(errorMessage);
        }
        setApiSummary(summary as IApiSummarySoap);
      } catch (e) {
        setError(e as Error);
      } finally {
        setLoading(false);
      }
    };
    fetchSummary();
  }, [specification, apiContext, setLoading, setApiSummary, setError]);

  return { apiSummary, loading, error };
}
