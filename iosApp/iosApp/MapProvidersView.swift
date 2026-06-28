import SwiftUI
import ComposeApp

struct MapProvidersView: View {
    @Environment(SettingsBridge.self) private var settings
    @State private var pendingApiKeyProvider: TileProviderConfig? = nil
    @State private var showAddCustom = false
    @State private var customToDelete: CustomProviderConfig? = nil

    private var activeId: String {
        settings.settings?.mapProviderId ?? "openstreetmap"
    }

    private var apiKeys: [String: String] {
        settings.settings?.apiKeys ?? [:]
    }

    private var customProviders: [CustomProviderConfig] {
        settings.settings?.customProviders ?? []
    }

    var body: some View {
        List {
            Section("Free") {
                ForEach(KoinHelpersKt.getBuiltInTileProviders(), id: \.id) { provider in
                    Button {
                        KoinHelpersKt.updateMapProviderById(id: provider.id)
                    } label: {
                        ProviderRow(name: provider.resolvedDisplayName, isSelected: provider.id == activeId)
                    }
                }
            }

            Section("API Key Required") {
                ForEach(KoinHelpersKt.getApiKeyTileProviders(), id: \.id) { provider in
                    let keyGroup = provider.apiKeyGroup ?? provider.id
                    let hasKey = apiKeys[keyGroup] != nil
                    Button {
                        if hasKey {
                            KoinHelpersKt.updateMapProviderById(id: provider.id)
                        } else {
                            pendingApiKeyProvider = provider
                        }
                    } label: {
                        HStack {
                            ProviderRow(name: provider.resolvedDisplayName, isSelected: provider.id == activeId)
                            Text(hasKey ? "Key set" : "Key required")
                                .font(.caption)
                                .foregroundStyle(hasKey ? .secondary : .orange)
                        }
                    }
                }
            }

            if !customProviders.isEmpty {
                Section("Custom") {
                    ForEach(customProviders, id: \.id) { custom in
                        HStack {
                            Button {
                                KoinHelpersKt.updateMapProviderById(id: custom.id)
                            } label: {
                                VStack(alignment: .leading, spacing: 2) {
                                    ProviderRow(name: custom.displayName, isSelected: custom.id == activeId)
                                    Text(custom.urlTemplate)
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                        .lineLimit(1)
                                }
                            }
                            Spacer()
                            Button(role: .destructive) {
                                customToDelete = custom
                            } label: {
                                Image(systemName: "trash")
                            }
                            .buttonStyle(.plain)
                            .foregroundStyle(.red)
                        }
                    }
                }
            }
        }
        .navigationTitle("Map Provider")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Add Custom") { showAddCustom = true }
            }
        }
        .sheet(isPresented: Binding(
            get: { pendingApiKeyProvider != nil },
            set: { if !$0 { pendingApiKeyProvider = nil } }
        )) {
            if let provider = pendingApiKeyProvider {
                ApiKeySheet(provider: provider, onDismiss: { pendingApiKeyProvider = nil })
            }
        }
        .sheet(isPresented: $showAddCustom) {
            AddCustomProviderSheet(isPresented: $showAddCustom)
        }
        .alert("Delete Provider?", isPresented: Binding(
            get: { customToDelete != nil },
            set: { if !$0 { customToDelete = nil } }
        )) {
            Button("Delete", role: .destructive) {
                if let custom = customToDelete {
                    KoinHelpersKt.deleteCustomTileProvider(id: custom.id)
                }
                customToDelete = nil
            }
            Button("Cancel", role: .cancel) { customToDelete = nil }
        } message: {
            if let custom = customToDelete {
                Text("Delete \"\(custom.displayName)\"?")
            }
        }
    }
}

// MARK: - Sub-views

private struct ProviderRow: View {
    let name: String
    let isSelected: Bool

    var body: some View {
        HStack {
            Text(name).foregroundStyle(.primary)
            Spacer()
            if isSelected {
                Image(systemName: "checkmark")
                    .foregroundStyle(.accentColor)
                    .fontWeight(.semibold)
            }
        }
    }
}

private struct ApiKeySheet: View {
    let provider: TileProviderConfig
    let onDismiss: () -> Void
    @State private var key = ""

    private var keyGroup: String { provider.apiKeyGroup ?? provider.id }
    private var providerGroupName: String {
        switch provider.apiKeyGroup {
        case "stadia": return "Stadia Maps"
        case "thunderforest": return "Thunderforest"
        case "jawg": return "Jawg"
        default: return provider.resolvedDisplayName
        }
    }
    private var keyInfo: String {
        switch provider.apiKeyGroup {
        case "stadia": return "Get a free API key at stadiamaps.com."
        case "thunderforest": return "Get an API key at thunderforest.com."
        case "jawg": return "Get an API key at jawg.io."
        default: return "Enter the API key for this provider."
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(keyInfo).foregroundStyle(.secondary)
                }
                Section("API Key") {
                    TextField("Paste your key here", text: $key)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                }
            }
            .navigationTitle(providerGroupName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        KoinHelpersKt.setApiKeyAndActivate(
                            keyGroup: keyGroup,
                            key: key,
                            providerId: provider.id
                        )
                        onDismiss()
                    }
                    .disabled(key.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }
}

private struct AddCustomProviderSheet: View {
    @Binding var isPresented: Bool
    @State private var name = ""
    @State private var urlTemplate = ""

    private var urlPreview: String {
        urlTemplate
            .replacingOccurrences(of: "{z}", with: "10")
            .replacingOccurrences(of: "{x}", with: "512")
            .replacingOccurrences(of: "{y}", with: "512")
    }
    private var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
            urlTemplate.contains("{z}") &&
            urlTemplate.contains("{x}") &&
            urlTemplate.contains("{y}")
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Provider Details") {
                    TextField("Name", text: $name).autocorrectionDisabled()
                    TextField("URL Template (use {z}, {x}, {y})", text: $urlTemplate)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .keyboardType(.URL)
                }
                if !urlTemplate.isEmpty {
                    Section("Preview") {
                        Text(urlPreview).font(.caption).foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("Add Custom Provider")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { isPresented = false }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        KoinHelpersKt.addCustomTileProvider(
                            id: UUID().uuidString.lowercased(),
                            name: name.trimmingCharacters(in: .whitespaces),
                            urlTemplate: urlTemplate.trimmingCharacters(in: .whitespaces)
                        )
                        isPresented = false
                    }
                    .disabled(!isValid)
                }
            }
        }
    }
}
