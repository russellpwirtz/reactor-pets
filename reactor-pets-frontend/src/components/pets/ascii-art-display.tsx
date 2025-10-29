export function AsciiArtDisplay({ art }: { art: string }) {
  return (
    <pre className="font-mono text-xs bg-muted p-4 rounded-lg overflow-x-auto">
      {art}
    </pre>
  );
}
