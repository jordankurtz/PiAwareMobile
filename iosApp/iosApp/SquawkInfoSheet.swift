import ComposeApp
import SwiftUI

struct SquawkInfoSheet: View {
    let squawk: String
    @Environment(\.dismiss) private var dismiss

    private var info: SquawkInfo? {
        SquawkCodes.shared.get(code: squawk)
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                if let severity = info?.severity, severity != .normal {
                    Text(severity.label)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(severityColor(severity))
                        .foregroundStyle(.white)
                        .clipShape(Capsule())
                }

                Text(info?.name ?? "Unknown Code")
                    .font(.headline)

                Text(info?.description_ ?? "No specific meaning is assigned to this squawk code.")
                    .font(.body)
                    .foregroundStyle(.secondary)

                Spacer()
            }
            .padding()
            .navigationTitle(squawk)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Dismiss") { dismiss() }
                }
            }
        }
    }

    private func severityColor(_ severity: SquawkSeverity) -> Color {
        switch severity {
        case .emergency: return .red
        case .caution: return .orange
        default: return .blue
        }
    }
}
